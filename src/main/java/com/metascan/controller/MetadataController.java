package com.metascan.controller;

import com.metascan.dto.MetadataExtractResponseDto;
import com.metascan.dto.spoof.SpoofAction;
import com.metascan.dto.spoof.SpoofRequestDto;
import com.metascan.service.MetadataCleaningService;
import com.metascan.service.MetadataExtractionService;
import com.metascan.service.MetadataLocationUpdateService;
import com.metascan.service.MetadataSpoofingService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/metadata")
public class MetadataController {

    private final MetadataExtractionService metadataExtractionService;
    private final MetadataCleaningService metadataCleaningService;
    private final MetadataLocationUpdateService metadataLocationUpdateService;
    private final MetadataSpoofingService metadataSpoofingService;

    public MetadataController(
            MetadataExtractionService metadataExtractionService,
            MetadataCleaningService metadataCleaningService,
            MetadataLocationUpdateService metadataLocationUpdateService,
            MetadataSpoofingService metadataSpoofingService
    ) {
        this.metadataExtractionService = metadataExtractionService;
        this.metadataCleaningService = metadataCleaningService;
        this.metadataLocationUpdateService = metadataLocationUpdateService;
        this.metadataSpoofingService = metadataSpoofingService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MetadataExtractResponseDto> extractMetadata(@RequestParam("file") @NotNull MultipartFile file) {
        MetadataExtractResponseDto response = metadataExtractionService.extract(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/clean", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> cleanMetadata(@RequestParam("file") @NotNull MultipartFile file) {
        MetadataCleaningService.CleanedFile cleanedFile = metadataCleaningService.clean(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(cleanedFile.fileName()))
                .contentType(resolveMediaType(cleanedFile.contentType()))
                .contentLength(cleanedFile.content().length)
                .body(cleanedFile.content());
    }

    @PostMapping(value = "/location/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> updateLocation(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("action") @NotNull String action,
            @RequestParam(value = "latitude", required = false) String latitude,
            @RequestParam(value = "longitude", required = false) String longitude
    ) {
        MetadataLocationUpdateService.UpdatedLocationFile updatedFile = metadataLocationUpdateService.updateLocation(
                file,
                action,
                latitude,
                longitude
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(updatedFile.fileName()))
                .contentType(resolveMediaType(updatedFile.contentType()))
                .contentLength(updatedFile.content().length)
                .body(updatedFile.content());
    }

    @PostMapping(value = "/spoof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> spoofMetadata(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam("action") @NotNull String action,
            @RequestParam(value = "latitude", required = false) String latitude,
            @RequestParam(value = "longitude", required = false) String longitude,
            @RequestParam(value = "newDate", required = false) String newDate,
            @RequestParam(value = "author", required = false) String author
    ) {
        SpoofRequestDto request = new SpoofRequestDto(
                SpoofAction.fromWireValue(action),
                latitude,
                longitude,
                newDate,
                author
        );

        MetadataSpoofingService.SpoofedFile spoofedFile = metadataSpoofingService.spoof(file, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(spoofedFile.fileName()))
                .contentType(resolveMediaType(spoofedFile.contentType()))
                .contentLength(spoofedFile.content().length)
                .body(spoofedFile.content());
    }

    private String buildContentDisposition(String fileName) {
        String safeFileName = fileName == null ? "metascan-clean-image" : fileName.replace("\"", "");
        return "attachment; filename=\"" + safeFileName + "\"";
    }

    private MediaType resolveMediaType(String rawContentType) {
        if (rawContentType == null || rawContentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(rawContentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
