package com.metascan.service;

import com.metascan.dto.MetadataExtractResponseDto;
import com.metascan.dto.MergedMetadataEntryDto;
import com.metascan.dto.MetadataSecurityDto;
import com.metascan.dto.MetadataSummaryDto;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MetadataExtractionService {

    private static final DateTimeFormatter HUMAN_READABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter EXIF_DATE_TIME_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX");
    private static final DateTimeFormatter EXIF_DATE_TIME_NO_ZONE = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private final AutoDetectParser parser = new AutoDetectParser();
    private final Tika tika = new Tika();
    private final ExifToolService exifToolService;

    public MetadataExtractionService(ExifToolService exifToolService) {
        this.exifToolService = exifToolService;
    }

    public MetadataExtractResponseDto extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo nao enviado ou vazio.");
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");

        try {
            byte[] fileBytes = file.getBytes();
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

            String author = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "dc:creator", "Author", "Creator");
            String lastAuthor = findFirstMetadataValue(extractedMetadata, exiftoolMetadata, "meta:last-author", "LastModifiedBy");
            String createdAt = formatDate(findFirstMetadataValue(
                    extractedMetadata,
                    exiftoolMetadata,
                    "dcterms:created",
                    "Creation-Date",
                    "meta:creation-date",
                    "CreateDate",
                    "DateCreated"
            ));
            String lastModified = formatDate(findFirstMetadataValue(
                    extractedMetadata,
                    exiftoolMetadata,
                    "dcterms:modified",
                    "Last-Modified",
                    "meta:save-date",
                    "ModifyDate"
            ));
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

            MetadataSecurityDto security = new MetadataSecurityDto(
                    !extractedMetadata.isEmpty(),
                    textLength > 0,
                    hasText(author),
                    hasText(createdAt)
            );

            return new MetadataExtractResponseDto(
                    fileName,
                    file.getSize(),
                    detectedContentType,
                    resolveFriendlyFileType(detectedContentType),
                    extractedMetadata,
                    exiftoolMetadata,
                    mergedMetadata,
                    textPreview,
                    textLength,
                    calculateSha256(fileBytes),
                    summary,
                    security,
                    exifToolResult.status()
            );
        } catch (IOException | TikaException | SAXException ex) {
            throw new MetadataExtractionException("Falha ao extrair metadados do arquivo.", ex);
        }
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

    private String formatDate(String rawDate) {
        if (!hasText(rawDate)) {
            return null;
        }

        return parseDate(rawDate)
                .map(date -> HUMAN_READABLE_DATE_FORMATTER.format(date))
                .orElse(rawDate);
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
}
