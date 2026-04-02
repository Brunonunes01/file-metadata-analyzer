package com.metascan.exception;

import com.metascan.dto.AntivirusScanStatus;

public class AntivirusScanException extends RuntimeException {

    private final AntivirusScanStatus status;
    private final String rawOutput;

    public AntivirusScanException(String message, AntivirusScanStatus status, String rawOutput) {
        super(message);
        this.status = status;
        this.rawOutput = rawOutput;
    }

    public AntivirusScanStatus getStatus() {
        return status;
    }

    public String getRawOutput() {
        return rawOutput;
    }
}
