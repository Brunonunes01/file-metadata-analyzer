package com.metascan.controller;

import com.metascan.dto.MetadataExtractResponseDto;
import com.metascan.service.MetadataExtractionService;
import jakarta.validation.constraints.NotNull;
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

    public MetadataController(MetadataExtractionService metadataExtractionService) {
        this.metadataExtractionService = metadataExtractionService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MetadataExtractResponseDto> extractMetadata(@RequestParam("file") @NotNull MultipartFile file) {
        MetadataExtractResponseDto response = metadataExtractionService.extract(file);
        return ResponseEntity.ok(response);
    }
}
