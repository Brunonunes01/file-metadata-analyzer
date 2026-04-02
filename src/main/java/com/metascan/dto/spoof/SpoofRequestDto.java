package com.metascan.dto.spoof;

public record SpoofRequestDto(
        SpoofAction action,
        CleanupMode cleanupMode,
        String latitude,
        String longitude,
        String newDate,
        String author
) {
}
