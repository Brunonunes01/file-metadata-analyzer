package com.metascan.service;

import com.metascan.exception.BadRequestException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
public class FileValidationService {

    public static final long MAX_UPLOAD_BYTES = 50L * 1024L * 1024L;
    private static final Set<String> GLOBAL_ALLOWED_EXACT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );
    private static final String ALLOWED_DOCX_PREFIX = "application/vnd.openxmlformats-officedocument.";

    private final Tika tika = new Tika();

    public ValidatedFile validateAndRead(MultipartFile file, Set<String> allowedExactTypes, String invalidTypeMessage) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo nao enviado ou vazio.");
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("Arquivo excede o limite de 50MB.");
        }

        String originalFileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();

        try {
            byte[] fileBytes = file.getBytes();
            String detectedContentType = tika.detect(fileBytes, originalFileName);

            if (isUnknownContentType(detectedContentType)) {
                throw new BadRequestException("Tipo real do arquivo nao reconhecido.");
            }

            if (!isAllowedType(detectedContentType, allowedExactTypes)) {
                throw new BadRequestException(invalidTypeMessage);
            }

            return new ValidatedFile(fileBytes, originalFileName, detectedContentType, file.getSize());
        } catch (IOException ex) {
            throw new BadRequestException("Falha ao ler o arquivo enviado.");
        }
    }

    public Set<String> globalAllowedExactTypes() {
        return GLOBAL_ALLOWED_EXACT_TYPES;
    }

    public boolean isAllowedType(String detectedContentType, Set<String> allowedExactTypes) {
        if (detectedContentType == null) {
            return false;
        }
        if (allowedExactTypes.contains(detectedContentType)) {
            return true;
        }
        return detectedContentType.startsWith(ALLOWED_DOCX_PREFIX);
    }

    private boolean isUnknownContentType(String contentType) {
        return contentType == null
                || contentType.isBlank()
                || "application/octet-stream".equalsIgnoreCase(contentType);
    }

    public record ValidatedFile(
            byte[] bytes,
            String originalFileName,
            String detectedContentType,
            long size
    ) {
    }
}
