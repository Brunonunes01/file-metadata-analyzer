package com.metascan.service;

import com.metascan.dto.AntivirusScanStatus;
import com.metascan.exception.BadRequestException;
import com.metascan.exception.MetadataExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Service
public class MetadataCleaningService {

    private static final Logger log = LoggerFactory.getLogger(MetadataCleaningService.class);
    private static final int EXIFTOOL_TIMEOUT_SECONDS = 60;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private final SecureTempFileService secureTempFileService;
    private final FileValidationService fileValidationService;
    private final AntivirusScanService antivirusScanService;
    private final SecureExecutionService secureExecutionService;

    public MetadataCleaningService(
            SecureTempFileService secureTempFileService,
            FileValidationService fileValidationService,
            AntivirusScanService antivirusScanService,
            SecureExecutionService secureExecutionService
    ) {
        this.secureTempFileService = secureTempFileService;
        this.fileValidationService = fileValidationService;
        this.antivirusScanService = antivirusScanService;
        this.secureExecutionService = secureExecutionService;
    }

    public CleanedFile clean(MultipartFile file) {
        FileValidationService.ValidatedFile validatedFile = fileValidationService.validateAndRead(
                file,
                SUPPORTED_IMAGE_TYPES,
                "Limpeza de metadados disponivel apenas para imagens JPEG e PNG."
        );
        String originalFileName = validatedFile.originalFileName();
        log.info("Upload recebido para limpeza de metadados: fileName='{}', size={} bytes, mime={}",
                originalFileName, validatedFile.size(), validatedFile.detectedContentType());
        Path tempFile = null;

        try {
            String detectedContentType = validatedFile.detectedContentType();
            tempFile = secureTempFileService.createSecureTempFile("metascan-clean-", ".tmp");
            Files.write(tempFile, validatedFile.bytes());

            enforceAntivirus(tempFile);

            runExifToolClean(tempFile);
            byte[] cleanedBytes = Files.readAllBytes(tempFile);

            return new CleanedFile(
                    cleanedBytes,
                    buildCleanFileName(originalFileName, detectedContentType),
                    detectedContentType
            );
        } catch (IOException ex) {
            throw new MetadataExtractionException("Falha ao processar arquivo para limpeza de metadados.", ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void enforceAntivirus(Path tempFile) {
        var scanResult = antivirusScanService.scan(tempFile);
        if (scanResult.isClean()) {
            return;
        }
        if (scanResult.status() == AntivirusScanStatus.INFECTED) {
            log.warn("Arquivo bloqueado por malware detectado em /metadata/clean.");
            throw new BadRequestException("Arquivo bloqueado por seguranca: possivel ameaca detectada pelo antivirus.");
        }
        throw new MetadataExtractionException("Scanner antivirus indisponivel para processar o arquivo.", new RuntimeException("antivirus"));
    }

    private void runExifToolClean(Path filePath) {
        List<String> commandArgs = List.of(
                "exiftool",
                "-overwrite_original",
                "-all=",
                "-P",
                filePath.toAbsolutePath().toString()
        );

        try {
            SecureExecutionService.CommandResult commandResult = secureExecutionService.execute(commandArgs, EXIFTOOL_TIMEOUT_SECONDS);
            if (commandResult.timedOut()) {
                throw new MetadataExtractionException(
                        "ExifTool excedeu o tempo limite durante a limpeza.",
                        new RuntimeException("ExifTool timeout")
                );
            }

            if (commandResult.exitCode() != 0) {
                throw new MetadataExtractionException(
                        buildErrorMessage("Falha ao remover metadados da imagem.", commandResult.stderr()),
                        new RuntimeException("ExifTool exit code diferente de zero")
                );
            }
        } catch (IOException ex) {
            if (isExifToolUnavailable(ex)) {
                throw new MetadataExtractionException("ExifTool nao instalado no sistema.", ex);
            }
            throw new MetadataExtractionException("Falha ao executar ExifTool para limpeza de metadados.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MetadataExtractionException("Execucao do ExifTool interrompida durante a limpeza.", ex);
        }
    }

    private boolean isExifToolUnavailable(IOException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("No such file") || message.contains("cannot find the file"));
    }

    private String buildErrorMessage(String prefix, String details) {
        if (details == null || details.isBlank()) {
            return prefix;
        }
        return prefix + " " + details.trim();
    }

    private String resolveTempExtension(String contentType) {
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        return ".img";
    }

    private String buildCleanFileName(String originalName, String contentType) {
        String sanitizedName = originalName == null || originalName.isBlank() ? "image" : originalName;
        int dotIndex = sanitizedName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
        String extension = dotIndex > 0 ? sanitizedName.substring(dotIndex) : resolveTempExtension(contentType);

        return baseName + "-clean" + extension;
    }

    public record CleanedFile(
            byte[] content,
            String fileName,
            String contentType
    ) {
    }
}
