package com.metascan.dto;

import java.util.Map;

public record MetadataResponseDto(
        String fileName,
        long fileSize,
        String contentTypeDetectado,
        Map<String, String> metadata
) {
}
