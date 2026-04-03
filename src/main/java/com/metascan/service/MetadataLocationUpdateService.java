package com.metascan.service;

import com.metascan.dto.AntivirusScanStatus;
import com.metascan.exception.BadRequestException;
import com.metascan.exception.MetadataExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MetadataLocationUpdateService {

    private static final Logger log = LoggerFactory.getLogger(MetadataLocationUpdateService.class);
    private static final int EXIFTOOL_TIMEOUT_SECONDS = 60;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private final SecureTempFileService secureTempFileService;
    private final FileValidationService fileValidationService;
    private final AntivirusScanService antivirusScanService;
    private final SecureExecutionService secureExecutionService;

    public MetadataLocationUpdateService(
            SecureTempFileService secureTempFileService,
            FileValidationService fileValidationService,
            AntivirusScanService antivirusScanService,
            SecureExecutionService secureExecutionService
    ) {
        this.secureTempFileService = secureTempFileService;
        this.fileValidationService = fileValidationService;
        this.antivirusScanService = antivirusScanService;
        this.secureExecutionService = secureExecutionService;
    }

    public UpdatedLocationFile updateLocation(
            MultipartFile file,
            String actionRaw,
            String latitudeRaw,
            String longitudeRaw
    ) {
        FileValidationService.ValidatedFile validatedFile = fileValidationService.validateAndRead(
                file,
                SUPPORTED_IMAGE_TYPES,
                "Atualizacao de localizacao disponivel apenas para imagens JPEG e PNG."
        );
        LocationAction action = parseAction(actionRaw);
        String originalFileName = validatedFile.originalFileName();
        log.info("Upload recebido para atualizacao de localizacao: fileName='{}', size={} bytes, mime={}, action={}",
                originalFileName, validatedFile.size(), validatedFile.detectedContentType(), action);
        Path tempFile = null;

        try {
            String detectedContentType = validatedFile.detectedContentType();
            tempFile = secureTempFileService.createSecureTempFile("metascan-location-", ".tmp");
            Files.write(tempFile, validatedFile.bytes());

            enforceAntivirus(tempFile);

            if (action == LocationAction.REMOVE) {
                runExifTool(buildRemoveArgs(tempFile));
            } else {
                Coordinates coordinates = parseAndValidateCoordinates(latitudeRaw, longitudeRaw);
                runExifTool(buildReplaceArgs(tempFile, coordinates));
            }

            byte[] updatedBytes = Files.readAllBytes(tempFile);
            return new UpdatedLocationFile(
                    updatedBytes,
                    buildOutputFileName(originalFileName, detectedContentType, action),
                    detectedContentType
            );
        } catch (IOException ex) {
            throw new MetadataExtractionException("Falha ao processar arquivo para atualizacao de localizacao GPS.", ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void enforceAntivirus(Path tempFile) {
        var scanResult = antivirusScanService.scan(tempFile);
        if (scanResult.isClean()) {
            return;
        }
        if (scanResult.status() == AntivirusScanStatus.INFECTED) {
            log.warn("Arquivo bloqueado por malware detectado em /metadata/location/update.");
            throw new BadRequestException("Arquivo bloqueado por seguranca: possivel ameaca detectada pelo antivirus.");
        }
        throw new MetadataExtractionException("Scanner antivirus indisponivel para processar o arquivo.", new RuntimeException("antivirus"));
    }

    private LocationAction parseAction(String actionRaw) {
        if (actionRaw == null || actionRaw.isBlank()) {
            throw new BadRequestException("Parametro 'action' obrigatorio. Valores aceitos: remove ou replace.");
        }

        String normalized = actionRaw.trim().toLowerCase(Locale.ROOT);
        if ("remove".equals(normalized)) {
            return LocationAction.REMOVE;
        }
        if ("replace".equals(normalized)) {
            return LocationAction.REPLACE;
        }

        throw new BadRequestException("Valor invalido para 'action'. Valores aceitos: remove ou replace.");
    }

    private Coordinates parseAndValidateCoordinates(String latitudeRaw, String longitudeRaw) {
        if (latitudeRaw == null || latitudeRaw.isBlank() || longitudeRaw == null || longitudeRaw.isBlank()) {
            throw new BadRequestException("Para action=replace, informe latitude e longitude.");
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

    private List<String> buildRemoveArgs(Path filePath) {
        List<String> args = new ArrayList<>();
        args.add("exiftool");
        args.add("-overwrite_original");
        args.add("-P");
        args.add("-gps:all=");
        args.add("-GPSLatitude=");
        args.add("-GPSLongitude=");
        args.add("-GPSLatitudeRef=");
        args.add("-GPSLongitudeRef=");
        args.add("-GPSPosition=");
        args.add("-GPSDateTime=");
        args.add("-GPSAltitude=");
        args.add("-GPSAltitudeRef=");
        args.add(filePath.toAbsolutePath().toString());
        return args;
    }

    private List<String> buildReplaceArgs(Path filePath, Coordinates coordinates) {
        double latitudeAbs = Math.abs(coordinates.latitude());
        double longitudeAbs = Math.abs(coordinates.longitude());
        String latitudeRef = coordinates.latitude() < 0 ? "S" : "N";
        String longitudeRef = coordinates.longitude() < 0 ? "W" : "E";

        String latValue = formatDecimal(latitudeAbs);
        String lonValue = formatDecimal(longitudeAbs);
        String gpsPositionValue = latValue + " " + latitudeRef + ", " + lonValue + " " + longitudeRef;

        List<String> args = new ArrayList<>();
        args.add("exiftool");
        args.add("-overwrite_original");
        args.add("-P");
        args.add("-GPSLatitude=" + latValue);
        args.add("-GPSLongitude=" + lonValue);
        args.add("-GPSLatitudeRef=" + latitudeRef);
        args.add("-GPSLongitudeRef=" + longitudeRef);
        args.add("-GPSPosition=" + gpsPositionValue);
        args.add(filePath.toAbsolutePath().toString());
        return args;
    }

    private void runExifTool(List<String> commandArgs) {
        try {
            SecureExecutionService.CommandResult commandResult = secureExecutionService.execute(commandArgs, EXIFTOOL_TIMEOUT_SECONDS);
            if (commandResult.timedOut()) {
                throw new MetadataExtractionException(
                        "ExifTool excedeu o tempo limite durante atualizacao da localizacao GPS.",
                        new RuntimeException("ExifTool timeout")
                );
            }

            if (commandResult.exitCode() != 0) {
                throw new MetadataExtractionException(
                        buildErrorMessage("Falha ao atualizar localizacao GPS da imagem.", commandResult.stderr()),
                        new RuntimeException("ExifTool exit code diferente de zero")
                );
            }
        } catch (IOException ex) {
            if (isExifToolUnavailable(ex)) {
                throw new MetadataExtractionException("ExifTool nao instalado no sistema.", ex);
            }
            throw new MetadataExtractionException("Falha ao executar ExifTool para atualizar localizacao GPS.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MetadataExtractionException("Execucao do ExifTool interrompida durante atualizacao da localizacao GPS.", ex);
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
        return ".img";
    }

    private String buildOutputFileName(String originalName, String contentType, LocationAction action) {
        String sanitizedName = originalName == null || originalName.isBlank() ? "image" : originalName;
        int dotIndex = sanitizedName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
        String extension = dotIndex > 0 ? sanitizedName.substring(dotIndex) : resolveTempExtension(contentType);
        String suffix = action == LocationAction.REMOVE ? "-no-gps" : "-relocated";
        return baseName + suffix + extension;
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private enum LocationAction {
        REMOVE,
        REPLACE
    }

    private record Coordinates(double latitude, double longitude) {
    }

    public record UpdatedLocationFile(
            byte[] content,
            String fileName,
            String contentType
    ) {
    }
}
