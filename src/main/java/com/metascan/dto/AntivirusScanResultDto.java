package com.metascan.dto;

public record AntivirusScanResultDto(
        AntivirusScanStatus status,
        String message,
        String rawOutput
) {
    public boolean isClean() {
        return status == AntivirusScanStatus.CLEAN;
    }
}
