package com.metascan.dto.spoof;

public record SpoofRequestDto(
        SpoofAction action,
        String latitude,
        String longitude,
        String newDate,
        String author
) {
}
