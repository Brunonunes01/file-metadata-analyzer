package com.metascan.dto.spoof;

import com.metascan.exception.BadRequestException;

import java.util.Locale;

public enum SpoofAction {
    REMOVE_GPS("remove_gps"),
    REPLACE_GPS("replace_gps"),
    CHANGE_DATE("change_date"),
    CHANGE_AUTHOR("change_author");

    private final String wireValue;

    SpoofAction(String wireValue) {
        this.wireValue = wireValue;
    }

    public static SpoofAction fromWireValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException(
                    "Parametro 'action' obrigatorio. Valores aceitos: remove_gps, replace_gps, change_date, change_author."
            );
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (SpoofAction action : values()) {
            if (action.wireValue.equals(normalized)) {
                return action;
            }
        }

        throw new BadRequestException(
                "Valor invalido para 'action'. Valores aceitos: remove_gps, replace_gps, change_date, change_author."
        );
    }
}
