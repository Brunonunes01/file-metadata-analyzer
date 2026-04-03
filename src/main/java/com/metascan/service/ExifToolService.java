package com.metascan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ExifToolService {

    private static final int EXIFTOOL_TIMEOUT_SECONDS = 60;
    private final ObjectMapper objectMapper;
    private final SecureExecutionService secureExecutionService;
    private final SecureTempFileService secureTempFileService;

    public ExifToolService(
            ObjectMapper objectMapper,
            SecureExecutionService secureExecutionService,
            SecureTempFileService secureTempFileService
    ) {
        this.objectMapper = objectMapper;
        this.secureExecutionService = secureExecutionService;
        this.secureTempFileService = secureTempFileService;
    }

    public ExifToolResult extractMetadata(byte[] fileBytes, String fileName) {
        Path tempFile = null;

        try {
            tempFile = secureTempFileService.createSecureTempFile("metascan-exif-", ".tmp");
            Files.write(tempFile, fileBytes);
            return runExifTool(tempFile);
        } catch (IOException ex) {
            return ExifToolResult.failure("error", "Falha ao preparar arquivo temporario para ExifTool.");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private ExifToolResult runExifTool(Path filePath) {
        List<String> commandArgs = List.of(
                "exiftool",
                "-j",
                "-charset",
                "filename=UTF8",
                filePath.toAbsolutePath().toString()
        );

        try {
            SecureExecutionService.CommandResult commandResult = secureExecutionService.execute(commandArgs, EXIFTOOL_TIMEOUT_SECONDS);
            if (commandResult.timedOut()) {
                return ExifToolResult.failure("timeout", "ExifTool excedeu o tempo limite.");
            }

            if (commandResult.exitCode() != 0) {
                return ExifToolResult.failure("error", buildErrorMessage("Falha na execucao do ExifTool.", commandResult.stderr()));
            }

            return parseExifToolJson(commandResult.stdout());
        } catch (IOException ex) {
            if (isExifToolUnavailable(ex)) {
                return ExifToolResult.failure("unavailable", "ExifTool nao instalado no sistema.");
            }
            return ExifToolResult.failure("error", "Falha ao executar ExifTool.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ExifToolResult.failure("error", "Execucao do ExifTool interrompida.");
        }
    }

    private ExifToolResult parseExifToolJson(String stdout) {
        try {
            JsonNode root = objectMapper.readTree(stdout);
            if (!root.isArray() || root.isEmpty()) {
                return ExifToolResult.failure("error", "Resposta JSON invalida do ExifTool.");
            }

            Map<String, Object> metadata = objectMapper.convertValue(root.get(0), new TypeReference<LinkedHashMap<String, Object>>() {
            });
            metadata.remove("SourceFile");

            return ExifToolResult.success(metadata);
        } catch (IOException ex) {
            return ExifToolResult.failure("error", "Falha ao interpretar JSON retornado pelo ExifTool.");
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

    public record ExifToolResult(
            Map<String, Object> metadata,
            String status,
            String message
    ) {
        static ExifToolResult success(Map<String, Object> metadata) {
            return new ExifToolResult(metadata, "success", null);
        }

        static ExifToolResult failure(String status, String message) {
            return new ExifToolResult(Map.of(), status, message);
        }

        public boolean isSuccess() {
            return "success".equals(status);
        }
    }
}
