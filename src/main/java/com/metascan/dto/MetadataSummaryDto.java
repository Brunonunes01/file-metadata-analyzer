package com.metascan.dto;

public record MetadataSummaryDto(
        String author,
        String lastAuthor,
        String createdAt,
        String lastModified,
        String title,
        String subject,
        String description,
        String language,
        String revision
) {
}
