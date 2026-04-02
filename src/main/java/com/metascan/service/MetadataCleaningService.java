package com.metascan.service;

import com.metascan.exception.BadRequestException;
import com.metascan.exception.MetadataExtractionException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class MetadataCleaningService {

    private static final int EXIFTOOL_TIMEOUT_SECONDS = 8;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private final Tika tika = new Tika();

    public CleanedFile clean(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo nao enviado ou vazio.");
        }

        String originalFileName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        Path tempFile = null;

        try {
            byte[] originalBytes = file.getBytes();
            String detectedContentType = tika.detect(originalBytes, originalFileName);
            validateSupportedImageType(detectedContentType);

            tempFile = Files.createTempFile("metascan-clean-", resolveTempExtension(detectedContentType));
            Files.write(tempFile, originalBytes);

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

    private void validateSupportedImageType(String contentType) {
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Limpeza de metadados disponivel apenas para imagens JPEG e PNG.");
        }
    }

    private void runExifToolClean(Path filePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "exiftool",
                "-overwrite_original",
                "-all=",
                "-P",
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
                throw new MetadataExtractionException(
                        "ExifTool excedeu o tempo limite durante a limpeza.",
                        new RuntimeException("ExifTool timeout")
                );
            }

            String stderr = getFutureValue(stderrFuture);
            getFutureValue(stdoutFuture);

            if (startedProcess.exitValue() != 0) {
                throw new MetadataExtractionException(
                        buildErrorMessage("Falha ao remover metadados da imagem.", stderr),
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
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            executorService.shutdownNow();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
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
