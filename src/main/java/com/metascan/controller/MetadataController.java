package com.metascan.controller;

import com.metascan.dto.MetadataExtractResponseDto;
import com.metascan.service.MetadataCleaningService;
import com.metascan.service.MetadataExtractionService;
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

    public MetadataController(
            MetadataExtractionService metadataExtractionService,
            MetadataCleaningService metadataCleaningService
    ) {
        this.metadataExtractionService = metadataExtractionService;
        this.metadataCleaningService = metadataCleaningService;
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
                .contentType(MediaType.parseMediaType(cleanedFile.contentType()))
                .contentLength(cleanedFile.content().length)
                .body(cleanedFile.content());
    }

    private String buildContentDisposition(String fileName) {
        String safeFileName = fileName == null ? "metascan-clean-image" : fileName.replace("\"", "");
        return "attachment; filename=\"" + safeFileName + "\"";
    }
}
