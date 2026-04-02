package com.metascan.service;

import com.metascan.dto.MetadataLocationDto;
import com.metascan.dto.MetadataPrivacyRiskDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MetadataPrivacyRiskService {

    public MetadataPrivacyRiskDto evaluate(
            Map<String, String> tikaMetadata,
            Map<String, Object> exiftoolMetadata,
            MetadataLocationDto location,
            boolean hasValidAuthor,
            boolean hasValidCreationDate
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> sensitiveDataFound = new ArrayList<>();

        if (location != null && location.hasGps()) {
            score += 5;
            reasons.add("A imagem contém localização GPS");
            sensitiveDataFound.add("GPS");
        }

        if (hasValidAuthor) {
            score += 2;
            reasons.add("O arquivo contém autor identificado");
            sensitiveDataFound.add("Author");
        }

        if (hasAnyMetadataValue(tikaMetadata, exiftoolMetadata,
                "Model", "Make", "CameraModelName", "DeviceModelName", "DeviceManufacturer",
                "tiff:Model", "tiff:Make")) {
            score += 2;
            reasons.add("O dispositivo usado para capturar o arquivo foi identificado");
            sensitiveDataFound.add("Device");
        }

        if (hasValidCreationDate || hasAnyMetadataValue(tikaMetadata, exiftoolMetadata,
                "DateTimeOriginal", "CreateDate", "SubSecDateTimeOriginal", "DateCreated",
                "dcterms:created", "Creation-Date")) {
            score += 1;
            reasons.add("O arquivo possui data original de criação/captura");
            sensitiveDataFound.add("CreateDate");
        }

        if (hasAnyMetadataValue(tikaMetadata, exiftoolMetadata,
                "ThumbnailImage", "ThumbnailLength", "PreviewImage", "JpgFromRaw",
                "PhotoshopThumbnail", "ThumbnailOffset", "ThumbnailSize")) {
            score += 2;
            reasons.add("O arquivo contém miniatura embutida");
            sensitiveDataFound.add("Thumbnail");
        }

        if (hasAnyMetadataValue(tikaMetadata, exiftoolMetadata,
                "Software", "Creator Tool", "CreatorTool", "xmp:CreatorTool",
                "Application-Name", "pdf:producer", "producer", "meta:generator")) {
            score += 2;
            reasons.add("A ferramenta de criação/edição do arquivo foi identificada");
            sensitiveDataFound.add("Software");
        }

        return new MetadataPrivacyRiskDto(
                resolveLevel(score),
                score,
                reasons,
                sensitiveDataFound
        );
    }

    private String resolveLevel(int score) {
        if (score <= 2) {
            return "low";
        }
        if (score <= 5) {
            return "medium";
        }
        return "high";
    }

    private boolean hasAnyMetadataValue(Map<String, String> tikaMetadata, Map<String, Object> exiftoolMetadata, String... keys) {
        for (String key : keys) {
            Object exifValue = exiftoolMetadata.get(key);
            if (hasText(exifValue)) {
                return true;
            }
        }

        for (String key : keys) {
            String tikaValue = tikaMetadata.get(key);
            if (hasText(tikaValue)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasText(Object value) {
        return value != null && !value.toString().isBlank();
    }
}
