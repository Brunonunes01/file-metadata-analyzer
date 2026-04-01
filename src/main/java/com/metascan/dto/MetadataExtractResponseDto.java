package com.metascan.dto;

import java.util.Map;

public record MetadataExtractResponseDto(
        String fileName,
        long fileSize,
        String contentTypeDetectado,
        String fileType,
        Map<String, String> tikaMetadata,
        Map<String, Object> exiftoolMetadata,
        Map<String, MergedMetadataEntryDto> mergedMetadata,
        String textPreview,
        int textLength,
        String hashSha256,
        MetadataSummaryDto summary,
        MetadataSecurityDto security,
        String exiftoolStatus
) {
}
