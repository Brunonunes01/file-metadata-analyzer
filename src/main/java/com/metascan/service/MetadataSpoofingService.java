package com.metascan.service;

import com.metascan.dto.spoof.SpoofAction;
import com.metascan.dto.spoof.CleanupMode;
import com.metascan.dto.spoof.SpoofRequestDto;
import com.metascan.exception.BadRequestException;
import com.metascan.exception.MetadataExtractionException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class MetadataSpoofingService {

    private static final int EXIFTOOL_TIMEOUT_SECONDS = 8;
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text",
            "text/plain"
    );
    private static final Set<String> GPS_SUPPORTED_TYPES = Set.of("image/jpeg", "image/png");
    private final Tika tika = new Tika();
    private final MetadataSpoofCleanupHelper metadataSpoofCleanupHelper;

    public MetadataSpoofingService(MetadataSpoofCleanupHelper metadataSpoofCleanupHelper) {
        this.metadataSpoofCleanupHelper = metadataSpoofCleanupHelper;
    }

    public SpoofedFile spoof(MultipartFile file, SpoofRequestDto request) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo nao enviado ou vazio.");
        }

        String originalFileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        Path sourceFile = null;
        Path spoofedFile = null;

        try {
            byte[] originalBytes = file.getBytes();
            String detectedContentType = tika.detect(originalBytes, originalFileName);
            validateSupportedContentType(detectedContentType);

            sourceFile = Files.createTempFile("metascan-spoof-source-", resolveTempExtension(detectedContentType));
            Files.write(sourceFile, originalBytes);
            spoofedFile = Files.createTempFile("metascan-spoof-target-", resolveTempExtension(detectedContentType));
            Files.copy(sourceFile, spoofedFile, StandardCopyOption.REPLACE_EXISTING);

            List<String> commandArgs = buildExifToolCommand(spoofedFile, detectedContentType, request);
            runExifTool(commandArgs);

            byte[] spoofedBytes = Files.readAllBytes(spoofedFile);
            return new SpoofedFile(
                    spoofedBytes,
                    buildSpoofedFileName(originalFileName, detectedContentType),
                    detectedContentType
            );
        } catch (IOException ex) {
            throw new MetadataExtractionException("Falha ao processar arquivo para spoofing de metadados.", ex);
        } finally {
            deleteTempFile(sourceFile);
            deleteTempFile(spoofedFile);
        }
    }

    private List<String> buildExifToolCommand(Path filePath, String detectedContentType, SpoofRequestDto request) {
        List<String> command = new ArrayList<>();
        command.add("exiftool");
        command.add("-overwrite_original");
        command.add("-P");
        command.add("-m");

        CleanupMode cleanupMode = request.cleanupMode() == null ? CleanupMode.PRESERVE : request.cleanupMode();
        command.addAll(metadataSpoofCleanupHelper.buildCleanupArgs(cleanupMode, request.action(), detectedContentType));

        if (request.action() == SpoofAction.REMOVE_GPS) {
            validateGpsSupportedType(detectedContentType);
            appendRemoveGpsArgs(command);
        } else if (request.action() == SpoofAction.REPLACE_GPS) {
            validateGpsSupportedType(detectedContentType);
            Coordinates coordinates = parseAndValidateCoordinates(request.latitude(), request.longitude());
            appendReplaceGpsArgs(command, coordinates);
        } else if (request.action() == SpoofAction.CHANGE_DATE) {
            appendChangeDateArgs(command, parseAndValidateDate(request.newDate()));
        } else if (request.action() == SpoofAction.CHANGE_AUTHOR) {
            appendChangeAuthorArgs(command, parseAndValidateAuthor(request.author()));
        } else {
            throw new BadRequestException("Acao de spoofing nao suportada.");
        }

        command.add(filePath.toAbsolutePath().toString());
        return command;
    }

    private void validateSupportedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Nao foi possivel identificar o tipo do arquivo enviado.");
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Tipo de arquivo nao suportado para spoofing. Tipos suportados: JPEG, PNG, PDF, DOC, DOCX, ODT e TXT."
            );
        }
    }

    private void validateGpsSupportedType(String contentType) {
        if (!GPS_SUPPORTED_TYPES.contains(contentType)) {
            throw new BadRequestException("Acoes de GPS disponiveis apenas para imagens JPEG e PNG.");
        }
    }

    private Coordinates parseAndValidateCoordinates(String latitudeRaw, String longitudeRaw) {
        if (latitudeRaw == null || latitudeRaw.isBlank() || longitudeRaw == null || longitudeRaw.isBlank()) {
            throw new BadRequestException("Para action=replace_gps, informe latitude e longitude.");
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latitudeRaw.trim());
            longitude = Double.parseDouble(longitudeRaw.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Latitude e longitude devem ser numeros validos.");
        }

        if (latitude < -90 || latitude > 90) {
            throw new BadRequestException("Latitude deve estar entre -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new BadRequestException("Longitude deve estar entre -180 e 180.");
        }

        return new Coordinates(latitude, longitude);
    }

    private LocalDateTime parseAndValidateDate(String newDateRaw) {
        if (newDateRaw == null || newDateRaw.isBlank()) {
            throw new BadRequestException("Para action=change_date, informe o campo newDate.");
        }

        String value = newDateRaw.trim();
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            DateTimeFormatter simpleFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(value, simpleFormat);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }

        throw new BadRequestException("Formato de newDate invalido. Use ISO ou yyyy-MM-dd HH:mm:ss.");
    }

    private String parseAndValidateAuthor(String authorRaw) {
        if (authorRaw == null || authorRaw.isBlank()) {
            throw new BadRequestException("Para action=change_author, informe o campo author.");
        }
        return authorRaw.trim();
    }

    private void appendRemoveGpsArgs(List<String> command) {
        command.add("-gps:all=");
        command.add("-GPSLatitude=");
        command.add("-GPSLongitude=");
        command.add("-GPSLatitudeRef=");
        command.add("-GPSLongitudeRef=");
        command.add("-GPSPosition=");
        command.add("-GPSDateTime=");
        command.add("-GPSAltitude=");
        command.add("-GPSAltitudeRef=");
    }

    private void appendReplaceGpsArgs(List<String> command, Coordinates coordinates) {
        double latitudeAbs = Math.abs(coordinates.latitude());
        double longitudeAbs = Math.abs(coordinates.longitude());
        String latitudeRef = coordinates.latitude() < 0 ? "S" : "N";
        String longitudeRef = coordinates.longitude() < 0 ? "W" : "E";

        String latValue = formatDecimal(latitudeAbs);
        String lonValue = formatDecimal(longitudeAbs);
        String gpsPosition = latValue + " " + latitudeRef + ", " + lonValue + " " + longitudeRef;

        command.add("-GPSLatitude=" + latValue);
        command.add("-GPSLongitude=" + lonValue);
        command.add("-GPSLatitudeRef=" + latitudeRef);
        command.add("-GPSLongitudeRef=" + longitudeRef);
        command.add("-GPSPosition=" + gpsPosition);
    }

    private void appendChangeDateArgs(List<String> command, LocalDateTime dateTime) {
        String exifDate = dateTime.format(EXIF_DATE_FORMAT);
        String isoDate = dateTime.atOffset(ZoneOffset.UTC).toString();

        command.add("-DateTimeOriginal=" + exifDate);
        command.add("-CreateDate=" + exifDate);
        command.add("-ModifyDate=" + exifDate);
        command.add("-dcterms:created=" + isoDate);
        command.add("-dcterms:modified=" + isoDate);
    }

    private void appendChangeAuthorArgs(List<String> command, String author) {
        command.add("-Artist=" + author);
        command.add("-Author=" + author);
        command.add("-Creator=" + author);
    }

    private void runExifTool(List<String> commandArgs) {
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Process process = null;

        try {
            Process startedProcess = processBuilder.start();
            process = startedProcess;

            Future<String> stdoutFuture = executorService.submit(() -> readStream(startedProcess.getInputStream()));
            Future<String> stderrFuture = executorService.submit(() -> readStream(startedProcess.getErrorStream()));

            boolean finished = startedProcess.waitFor(EXIFTOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                startedProcess.destroyForcibly();
                throw new MetadataExtractionException(
                        "ExifTool excedeu o tempo limite durante o spoofing de metadados.",
                        new RuntimeException("ExifTool timeout")
                );
            }

            String stderr = getFutureValue(stderrFuture);
            getFutureValue(stdoutFuture);

            if (startedProcess.exitValue() != 0) {
                throw new MetadataExtractionException(
                        buildErrorMessage("Falha ao executar spoofing de metadados.", stderr),
                        new RuntimeException("ExifTool exit code diferente de zero")
                );
            }
        } catch (IOException ex) {
            if (isExifToolUnavailable(ex)) {
                throw new MetadataExtractionException("ExifTool nao instalado no sistema.", ex);
            }
            throw new MetadataExtractionException("Falha ao executar ExifTool para spoofing de metadados.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MetadataExtractionException("Execucao do ExifTool interrompida durante spoofing.", ex);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            executorService.shutdownNow();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getFutureValue(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException ex) {
            return "";
        }
    }

    private boolean isExifToolUnavailable(IOException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("No such file") || message.contains("cannot find the file"));
    }

    private String buildErrorMessage(String prefix, String details) {
        if (details == null || details.isBlank()) {
            return prefix;
        }
        return prefix + " " + details.trim();
    }

    private String resolveTempExtension(String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("application/pdf".equals(contentType)) {
            return ".pdf";
        }
        if ("application/msword".equals(contentType)) {
            return ".doc";
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return ".docx";
        }
        if ("application/vnd.oasis.opendocument.text".equals(contentType)) {
            return ".odt";
        }
        if ("text/plain".equals(contentType)) {
            return ".txt";
        }
        return ".bin";
    }

    private String buildSpoofedFileName(String originalName, String contentType) {
        String sanitizedName = originalName == null || originalName.isBlank() ? "file" : originalName;
        int dotIndex = sanitizedName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
        String extension = dotIndex > 0 ? sanitizedName.substring(dotIndex) : resolveTempExtension(contentType);
        return "spoofed-" + baseName + extension;
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private void deleteTempFile(Path filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private record Coordinates(double latitude, double longitude) {
    }

    public record SpoofedFile(
            byte[] content,
            String fileName,
            String contentType
    ) {
    }
}
