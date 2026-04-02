package com.metascan.service;

import com.metascan.dto.AntivirusScanResultDto;
import com.metascan.dto.AntivirusScanStatus;
import com.metascan.dto.MetadataExtractResponseDto;
import com.metascan.dto.MetadataLocationDto;
import com.metascan.dto.MetadataPrivacyRiskDto;
import com.metascan.dto.MergedMetadataEntryDto;
import com.metascan.dto.MetadataSecurityDto;
import com.metascan.dto.MetadataSummaryDto;
import com.metascan.exception.AntivirusScanException;
import com.metascan.exception.AntivirusThreatDetectedException;
import com.metascan.exception.BadRequestException;
import com.metascan.exception.MetadataExtractionException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import org.apache.tika.sax.BodyContentHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class MetadataExtractionService {

    private static final DateTimeFormatter HUMAN_READABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter EXIF_DATE_TIME_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private static final DateTimeFormatter EXIF_DATE_TIME_NO_ZONE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private static final Pattern COORDINATE_NUMBER_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)");
    private static final Pattern COORDINATE_DIRECTION_PATTERN = Pattern.compile("(^|\\W)([NSEW])(\\W|$)");
    private final AutoDetectParser parser = new AutoDetectParser();
    private final Tika tika = new Tika();
    private final AntivirusScanService antivirusScanService;
    private final ExifToolService exifToolService;
    private final MetadataInsightsService metadataInsightsService;
    private final MetadataPrivacyRiskService metadataPrivacyRiskService;

    public MetadataExtractionService(
            AntivirusScanService antivirusScanService,
            ExifToolService exifToolService,
            MetadataInsightsService metadataInsightsService,
            MetadataPrivacyRiskService metadataPrivacyRiskService
    ) {
        this.antivirusScanService = antivirusScanService;
        this.exifToolService = exifToolService;
        this.metadataInsightsService = metadataInsightsService;
        this.metadataPrivacyRiskService = metadataPrivacyRiskService;
    }

    public MetadataExtractResponseDto extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo nao enviado ou vazio.");
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");

        try {
            byte[] fileBytes = file.getBytes();
            AntivirusScanResultDto antivirusScanResult = scanWithAntivirus(fileBytes, fileName);
            handleAntivirusResult(antivirusScanResult);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

            String detectedContentType = tika.detect(fileBytes, fileName);
            metadata.set(Metadata.CONTENT_TYPE, detectedContentType);

            BodyContentHandler contentHandler = new BodyContentHandler(-1);
            parser.parse(new ByteArrayInputStream(fileBytes), contentHandler, metadata, new ParseContext());

            Map<String, String> extractedMetadata = Arrays.stream(metadata.names())
                    .sorted()
                    .collect(Collectors.toMap(
                            name -> name,
                            metadata::get,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    ));

            ExifToolService.ExifToolResult exifToolResult = exifToolService.extractMetadata(fileBytes, fileName);
            Map<String, Object> exiftoolMetadata = new LinkedHashMap<>(exifToolResult.metadata());
            Map<String, MergedMetadataEntryDto> mergedMetadata = mergeMetadata(extractedMetadata, exiftoolMetadata);
            int metadataCount = mergedMetadata.size();

            String author = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:creator", "Author", "Creator");
            String lastAuthor = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "meta:last-author", "LastModifiedBy");
            DateResolution createdAtResolution = resolveBestDate(
                    extractedMetadata,
                    exiftoolMetadata,
                    "dcterms:created",
                    "Creation-Date",
                    "meta:creation-date",
                    "CreateDate",
                    "DateCreated"
            );
            DateResolution lastModifiedResolution = resolveBestDate(
                    extractedMetadata,
                    exiftoolMetadata,
                    "dcterms:modified",
                    "Last-Modified",
                    "meta:save-date",
                    "ModifyDate"
            );
            String createdAt = createdAtResolution.value();
            String lastModified = lastModifiedResolution.value();
            String title = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:title", "title", "Title");
            String subject = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:subject", "cp:subject", "subject", "Subject");
            String description = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:description", "description", "Description");
            String language = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:language", "language", "Content-Language", "Language");
            String revision = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "cp:revision", "meta:revision", "revision-number", "RevisionNumber");

            MetadataSummaryDto summary = new MetadataSummaryDto(
                    author,
                    lastAuthor,
                    createdAt,
                    lastModified,
                    title,
                    subject,
                    description,
                    language,
                    revision
            );

            String extractedText = normalizeText(contentHandler.toString());
            int textLength = extractedText.length();
            String textPreview = textLength == 0 ? "" : extractedText.substring(0, Math.min(1000, textLength));
            MetadataLocationDto location = buildLocation(exiftoolMetadata);
            boolean isImageFile = detectedContentType != null && detectedContentType.startsWith("image/");
            List<String> insights = metadataInsightsService.generateInsights(
                    author,
                    createdAtResolution.hasValidDate(),
                    textLength,
                    metadataCount,
                    isImageFile,
                    location.hasGps()
            );
            boolean hasValidAuthor = metadataInsightsService.isValidAuthor(author);
            MetadataPrivacyRiskDto privacyRisk = metadataPrivacyRiskService.evaluate(
                    extractedMetadata,
                    exiftoolMetadata,
                    location,
                    hasValidAuthor,
                    createdAtResolution.hasValidDate()
            );

            MetadataSecurityDto security = new MetadataSecurityDto(
                    metadataCount > 0,
                    textLength > 0,
                    hasValidAuthor,
                    createdAtResolution.hasValidDate()
            );

            return new MetadataExtractResponseDto(
                    fileName,
                    file.getSize(),
                    detectedContentType,
                    resolveFriendlyFileType(detectedContentType),
                    extractedMetadata,
                    exiftoolMetadata,
                    mergedMetadata,
                    metadataCount,
                    insights,
                    textPreview,
                    textLength,
                    calculateSha256(fileBytes),
                    summary,
                    security,
                    antivirusScanResult.status().wireValue(),
                    exifToolResult.status(),
                    location,
                    privacyRisk
            );
        } catch (IOException | TikaException | SAXException ex) {
            throw new MetadataExtractionException("Falha ao extrair metadados do arquivo.", ex);
        }
    }

    private AntivirusScanResultDto scanWithAntivirus(byte[] fileBytes, String fileName) {
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("metascan-av-", "-" + sanitizeFileName(fileName));
            Files.write(tempFile, fileBytes);
            return antivirusScanService.scan(tempFile);
        } catch (IOException ex) {
            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Nao foi possivel preparar o arquivo para verificacao antivirus.",
                    ex.getMessage()
            );
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void handleAntivirusResult(AntivirusScanResultDto scanResult) {
        AntivirusScanStatus status = scanResult.status();

        if (status == AntivirusScanStatus.CLEAN) {
            return;
        }

        if (status == AntivirusScanStatus.INFECTED) {
            throw new AntivirusThreatDetectedException(
                    "Arquivo bloqueado por seguranca: possivel ameaca detectada pelo antivirus."
            );
        }

        String message = switch (status) {
            case NOT_INSTALLED -> "Scanner antivirus indisponivel no momento.";
            case TIMEOUT -> "Nao foi possivel concluir a verificacao antivirus no tempo limite.";
            case SCAN_ERROR -> "Nao foi possivel concluir a verificacao antivirus do arquivo.";
            case CLEAN, INFECTED -> "Nao foi possivel concluir a verificacao antivirus do arquivo.";
        };

        throw new AntivirusScanException(message, status, scanResult.rawOutput());
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private MetadataLocationDto buildLocation(Map<String, Object> exiftoolMetadata) {
        String gpsLatitudeRaw = valueAsText(exiftoolMetadata.get("GPSLatitude"));
        String gpsLongitudeRaw = valueAsText(exiftoolMetadata.get("GPSLongitude"));
        String gpsPositionOriginal = valueAsText(exiftoolMetadata.get("GPSPosition"));
        String gpsDateTime = valueAsText(exiftoolMetadata.get("GPSDateTime"));
        String gpsAltitude = valueAsText(exiftoolMetadata.get("GPSAltitude"));

        Double latitudeDecimal = parseGpsCoordinate(gpsLatitudeRaw);
        Double longitudeDecimal = parseGpsCoordinate(gpsLongitudeRaw);

        if ((latitudeDecimal == null || longitudeDecimal == null) && hasText(gpsPositionOriginal)) {
            String[] parts = gpsPositionOriginal.split("\\s*,\\s*");
            if (parts.length >= 2) {
                if (latitudeDecimal == null) {
                    latitudeDecimal = parseGpsCoordinate(parts[0]);
                }
                if (longitudeDecimal == null) {
                    longitudeDecimal = parseGpsCoordinate(parts[1]);
                }
            }
        }

        boolean hasGps = hasText(gpsLatitudeRaw)
                || hasText(gpsLongitudeRaw)
                || hasText(gpsPositionOriginal)
                || hasText(gpsDateTime)
                || hasText(gpsAltitude);

        String mapsUrl = null;
        if (latitudeDecimal != null && longitudeDecimal != null) {
            mapsUrl = "https://www.google.com/maps?q=" + latitudeDecimal + "," + longitudeDecimal;
        }

        if (!hasText(gpsPositionOriginal) && hasText(gpsLatitudeRaw) && hasText(gpsLongitudeRaw)) {
            gpsPositionOriginal = gpsLatitudeRaw + ", " + gpsLongitudeRaw;
        }

        return new MetadataLocationDto(
                hasGps,
                latitudeDecimal,
                longitudeDecimal,
                gpsPositionOriginal,
                mapsUrl,
                gpsDateTime,
                gpsAltitude
        );
    }

    private Map<String, MergedMetadataEntryDto> mergeMetadata(Map<String, String> tikaMetadata, Map<String, Object> exiftoolMetadata) {
        Map<String, MergedMetadataEntryDto> merged = new LinkedHashMap<>();

        tikaMetadata.forEach((key, value) -> merged.put(key, new MergedMetadataEntryDto(value, "tika")));

        exiftoolMetadata.forEach((key, exifValue) -> {
            MergedMetadataEntryDto existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new MergedMetadataEntryDto(exifValue, "exiftool"));
                return;
            }

            if (Objects.equals(existing.value(), exifValue)) {
                merged.put(key, new MergedMetadataEntryDto(existing.value(), "both"));
                return;
            }

            Map<String, Object> values = new LinkedHashMap<>();
            values.put("tika", existing.value());
            values.put("exiftool", exifValue);
            merged.put(key, new MergedMetadataEntryDto(values, "both"));
        });

        return merged;
    }

    private String findFirstMetadataValue(Map<String, String> tikaMetadata, Map<String, Object> exiftoolMetadata, String... keys) {
        for (String key : keys) {
            Object exifValue = exiftoolMetadata.get(key);
            if (exifValue != null && hasText(exifValue.toString())) {
                return exifValue.toString().trim();
            }
        }

        for (String key : keys) {
            String value = tikaMetadata.get(key);
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private DateResolution resolveBestDate(Map<String, String> tikaMetadata, Map<String, Object> exiftoolMetadata, String... keys) {
        String fallbackRawDate = null;

        for (String key : keys) {
            Object exifValue = exiftoolMetadata.get(key);
            if (exifValue == null) {
                continue;
            }

            String rawDate = exifValue.toString().trim();
            if (!hasText(rawDate)) {
                continue;
            }

            if (fallbackRawDate == null) {
                fallbackRawDate = rawDate;
            }

            Optional<LocalDateTime> parsedDate = parseDate(rawDate);
            if (parsedDate.isPresent()) {
                return new DateResolution(HUMAN_READABLE_DATE_FORMATTER.format(parsedDate.get()), true);
            }
        }

        for (String key : keys) {
            String rawDate = tikaMetadata.get(key);
            if (!hasText(rawDate)) {
                continue;
            }

            String trimmedDate = rawDate.trim();
            if (fallbackRawDate == null) {
                fallbackRawDate = trimmedDate;
            }

            Optional<LocalDateTime> parsedDate = parseDate(trimmedDate);
            if (parsedDate.isPresent()) {
                return new DateResolution(HUMAN_READABLE_DATE_FORMATTER.format(parsedDate.get()), true);
            }
        }

        return new DateResolution(fallbackRawDate, false);
    }

    private Optional<LocalDateTime> parseDate(String rawDate) {
        try {
            return Optional.of(Instant.parse(rawDate).atZone(ZoneId.systemDefault()).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(OffsetDateTime.parse(rawDate).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(ZonedDateTime.parse(rawDate).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(LocalDateTime.parse(rawDate, DateTimeFormatter.ISO_DATE_TIME));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(LocalDate.parse(rawDate, DateTimeFormatter.ISO_DATE).atStartOfDay());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(OffsetDateTime.parse(rawDate, EXIF_DATE_TIME_WITH_ZONE).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return Optional.of(LocalDateTime.parse(rawDate, EXIF_DATE_TIME_NO_ZONE));
        } catch (DateTimeParseException ignored) {
        }

        return Optional.empty();
    }

    private String resolveFriendlyFileType(String contentType) {
        if (!hasText(contentType)) {
            return "Arquivo desconhecido";
        }

        if (contentType.equals("application/pdf")) {
            return "PDF";
        }
        if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || contentType.equals("application/msword")) {
            return "Documento Word";
        }
        if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || contentType.equals("application/vnd.ms-excel")) {
            return "Planilha Excel";
        }
        if (contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                || contentType.equals("application/vnd.ms-powerpoint")) {
            return "Apresentacao PowerPoint";
        }
        if (contentType.equals("image/jpeg")) {
            return "Imagem JPEG";
        }
        if (contentType.equals("image/png")) {
            return "Imagem PNG";
        }
        if (contentType.equals("text/plain")) {
            return "Texto simples";
        }
        if (contentType.startsWith("audio/")) {
            return "Audio";
        }
        if (contentType.startsWith("video/")) {
            return "Video";
        }
        return "Arquivo desconhecido";
    }

    private String normalizeText(String content) {
        if (!hasText(content)) {
            return "";
        }

        return content.replaceAll("\\s+", " ").trim();
    }

    private String calculateSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new MetadataExtractionException("Falha ao calcular hash do arquivo.", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueAsText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Double parseGpsCoordinate(String rawCoordinate) {
        if (!hasText(rawCoordinate)) {
            return null;
        }

        String normalized = rawCoordinate.trim().toUpperCase(Locale.ROOT);
        Character direction = extractDirection(normalized);

        List<Double> numericParts = new ArrayList<>();
        Matcher matcher = COORDINATE_NUMBER_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String numberToken = matcher.group(1).replace(',', '.');
            try {
                numericParts.add(Double.parseDouble(numberToken));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (numericParts.isEmpty()) {
            return null;
        }

        double degrees = numericParts.get(0);
        double minutes = numericParts.size() > 1 ? numericParts.get(1) : 0d;
        double seconds = numericParts.size() > 2 ? numericParts.get(2) : 0d;

        double decimal = degrees + (minutes / 60d) + (seconds / 3600d);
        if (direction != null && (direction == 'S' || direction == 'W')) {
            decimal = -Math.abs(decimal);
        } else if (direction != null && (direction == 'N' || direction == 'E')) {
            decimal = Math.abs(decimal);
        }

        return decimal;
    }

    private Character extractDirection(String normalizedCoordinate) {
        Matcher directionMatcher = COORDINATE_DIRECTION_PATTERN.matcher(normalizedCoordinate);
        if (directionMatcher.find()) {
            return directionMatcher.group(2).charAt(0);
        }
        return null;
    }

    private record DateResolution(
            String value,
            boolean hasValidDate
    ) {
    }
}
