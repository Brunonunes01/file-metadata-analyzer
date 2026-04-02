package com.metascan.dto;

import java.util.Map;
import java.util.List;

public record MetadataExtractResponseDto(
        String fileName,
        long fileSize,
        String contentTypeDetectado,
        String fileType,
        Map<String, String> tikaMetadata,
        Map<String, Object> exiftoolMetadata,
        Map<String, MergedMetadataEntryDto> mergedMetadata,
        int metadataCount,
        List<String> insights,
        String textPreview,
        int textLength,
        String hashSha256,
        MetadataSummaryDto summary,
        MetadataSecurityDto security,
        String exiftoolStatus,
        MetadataLocationDto location,
        MetadataPrivacyRiskDto privacyRisk
) {
}
