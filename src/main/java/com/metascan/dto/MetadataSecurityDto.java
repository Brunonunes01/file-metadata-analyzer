package com.metascan.dto;

public record MetadataSecurityDto(
        boolean hasMetadata,
        boolean hasText,
        boolean hasAuthor,
        boolean hasCreationDate
) {
}
