package com.metascan.dto;

public record MetadataLocationDto(
        boolean hasGps,
        Double latitudeDecimal,
        Double longitudeDecimal,
        String gpsPositionOriginal,
        String mapsUrl,
        String gpsDateTime,
        String gpsAltitude
) {
}
