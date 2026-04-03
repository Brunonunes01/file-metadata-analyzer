package com.metascan.service;

import com.metascan.dto.DeviceForensicsDto;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class DeviceForensicsService {

    public DeviceForensicsDto build(String detectedContentType, Map<String, Object> exiftoolMetadata) {
        if (!isImageFile(detectedContentType) || exiftoolMetadata == null || exiftoolMetadata.isEmpty()) {
            return DeviceForensicsDto.notDetected();
        }

        String brand = valueAsText(exiftoolMetadata.get("Make"));
        String xiaomiModel = valueAsText(exiftoolMetadata.get("XiaomiModel"));
        String modelRaw = valueAsText(exiftoolMetadata.get("Model"));
        String cameraType = normalizeCameraType(valueAsText(exiftoolMetadata.get("SensorType")));
        String aperture = firstAvailable(exiftoolMetadata, "FNumber", "Aperture");
        String iso = valueAsText(exiftoolMetadata.get("ISO"));
        String shutterSpeed = firstAvailable(exiftoolMetadata, "ExposureTime", "ShutterSpeed");
        String focalLength = valueAsText(exiftoolMetadata.get("FocalLength"));
        String imageSize = valueAsText(exiftoolMetadata.get("ImageSize"));
        String megapixels = valueAsText(exiftoolMetadata.get("Megapixels"));

        String model = hasText(xiaomiModel) ? xiaomiModel : modelRaw;
        String modelCode = hasText(xiaomiModel) ? modelRaw : null;

        boolean hasDeviceIdentity = hasText(brand) || hasText(model) || hasText(modelCode);
        if (!hasDeviceIdentity) {
            return DeviceForensicsDto.notDetected();
        }

        return new DeviceForensicsDto(
                true,
                brand,
                model,
                modelCode,
                cameraType,
                aperture,
                iso,
                shutterSpeed,
                focalLength,
                imageSize,
                megapixels
        );
    }

    private boolean isImageFile(String contentType) {
        return hasText(contentType) && contentType.startsWith("image/");
    }

    private String firstAvailable(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            String value = valueAsText(metadata.get(key));
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeCameraType(String sensorType) {
        if (!hasText(sensorType)) {
            return null;
        }

        String normalized = sensorType.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("rear")) {
            return "Câmera traseira";
        }
        if (normalized.equals("front")) {
            return "Câmera frontal";
        }
        return sensorType;
    }

    private String valueAsText(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
