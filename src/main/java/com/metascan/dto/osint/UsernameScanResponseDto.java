package com.metascan.dto.osint;

import java.time.Instant;
import java.util.List;

public record UsernameScanResponseDto(
        String username,
        Instant scannedAt,
        String scanStatus,
        String message,
        int totalProfilesFound,
        List<UsernameScanProfileDto> profiles
) {
}
