package com.metascan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ExifToolService {

    private static final int EXIFTOOL_TIMEOUT_SECONDS = 8;
    private final ObjectMapper objectMapper;

    public ExifToolService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExifToolResult extractMetadata(byte[] fileBytes, String fileName) {
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("metascan-", "-" + sanitizeFileName(fileName));
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
        ProcessBuilder processBuilder = new ProcessBuilder(
                "exiftool",
                "-j",
                "-charset",
                "filename=UTF8",
                filePath.toAbsolutePath().toString()
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Process process = null;

        try {
            Process startedProcess = processBuilder.start();
            process = startedProcess;

            Future<String> stdoutFuture = executorService.submit(() -> readStream(startedProcess.getInputStream()));
            Future<String> stderrFuture = executorService.submit(() -> readStream(startedProcess.getErrorStream()));

            boolean finished = startedProcess.waitFor(EXIFTOOL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                startedProcess.destroyForcibly();
                return ExifToolResult.failure("timeout", "ExifTool excedeu o tempo limite.");
            }

            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);

            if (startedProcess.exitValue() != 0) {
                return ExifToolResult.failure("error", buildErrorMessage("Falha na execucao do ExifTool.", stderr));
            }

            return parseExifToolJson(stdout);
        } catch (IOException ex) {
            if (isExifToolUnavailable(ex)) {
                return ExifToolResult.failure("unavailable", "ExifTool nao instalado no sistema.");
            }
            return ExifToolResult.failure("error", "Falha ao executar ExifTool.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ExifToolResult.failure("error", "Execucao do ExifTool interrompida.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            executorService.shutdownNow();
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

    private String getFutureValue(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException ex) {
            return "";
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
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

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
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
