package com.metascan.dto.spoof;

import com.metascan.exception.BadRequestException;

import java.util.Locale;

public enum CleanupMode {
    PRESERVE("preserve"),
    SENSITIVE("sensitive"),
    MAXIMUM("maximum");

    private final String wireValue;

    CleanupMode(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static CleanupMode fromWireValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PRESERVE;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (CleanupMode mode : values()) {
            if (mode.wireValue.equals(normalized)) {
                return mode;
            }
        }

        throw new BadRequestException(
                "Valor invalido para 'cleanupMode'. Valores aceitos: preserve, sensitive, maximum."
        );
    }
}
