package com.metascan.dto;

public enum AntivirusScanStatus {
    CLEAN("clean"),
    INFECTED("infected"),
    SCAN_ERROR("scan_error"),
    NOT_INSTALLED("not_installed"),
    TIMEOUT("timeout");

    private final String wireValue;

    AntivirusScanStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
