package com.metascan.dto;

public record DeviceForensicsDto(
        boolean deviceDetected,
        String brand,
        String model,
        String modelCode,
        String cameraType,
        String aperture,
        String iso,
        String shutterSpeed,
        String focalLength,
        String imageSize,
        String megapixels
) {
    public static DeviceForensicsDto notDetected() {
        return new DeviceForensicsDto(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
