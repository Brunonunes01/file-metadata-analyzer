package com.metascan.service;

import com.metascan.dto.spoof.CleanupMode;
import com.metascan.dto.spoof.SpoofAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetadataSpoofCleanupHelper {

    public List<String> buildCleanupArgs(CleanupMode cleanupMode, SpoofAction action, String detectedContentType) {
        if (cleanupMode == null || cleanupMode == CleanupMode.PRESERVE) {
            return List.of();
        }

        if (cleanupMode == CleanupMode.MAXIMUM) {
            return List.of("-all=");
        }

        List<String> args = new ArrayList<>();
        appendSensitiveCommonArgs(args);

        if (isImageType(detectedContentType)) {
            appendSensitiveImageArgs(args);
            appendGpsResidualArgs(args, action);
        }

        if (action == SpoofAction.CHANGE_DATE) {
            appendDateResidualArgs(args);
        }

        return args;
    }

    private void appendSensitiveCommonArgs(List<String> args) {
        args.add("-Artist=");
        args.add("-Author=");
        args.add("-Creator=");
        args.add("-OwnerName=");
        args.add("-Software=");
    }

    private void appendSensitiveImageArgs(List<String> args) {
        args.add("-Make=");
        args.add("-Model=");
        args.add("-SerialNumber=");
        args.add("-InternalSerialNumber=");
        args.add("-LensModel=");
        args.add("-ThumbnailImage=");
        args.add("-PreviewImage=");
        args.add("-JpgFromRaw=");
    }

    private void appendGpsResidualArgs(List<String> args, SpoofAction action) {
        if (action == SpoofAction.REMOVE_GPS) {
            args.add("-gps:all=");
            return;
        }

        args.add("-GPSDateStamp=");
        args.add("-GPSTimeStamp=");
        args.add("-GPSImgDirection=");
        args.add("-GPSImgDirectionRef=");
        args.add("-GPSDOP=");
        args.add("-GPSHPositioningError=");
        args.add("-GPSMapDatum=");
        args.add("-GPSProcessingMethod=");
        args.add("-GPSAreaInformation=");
        args.add("-GPSDestLatitude=");
        args.add("-GPSDestLongitude=");
        args.add("-GPSDestLatitudeRef=");
        args.add("-GPSDestLongitudeRef=");
        args.add("-GPSDestDistance=");
        args.add("-GPSDestDistanceRef=");
        args.add("-GPSDateTime=");
    }

    private void appendDateResidualArgs(List<String> args) {
        args.add("-DateTimeDigitized=");
        args.add("-SubSecDateTimeOriginal=");
        args.add("-SubSecCreateDate=");
        args.add("-SubSecModifyDate=");
        args.add("-OffsetTime=");
        args.add("-OffsetTimeOriginal=");
        args.add("-OffsetTimeDigitized=");
    }

    private boolean isImageType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
