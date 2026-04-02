package com.metascan.service;

import com.metascan.dto.AntivirusScanResultDto;
import com.metascan.dto.AntivirusScanStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AntivirusScanService {

    private static final int ANTIVIRUS_TIMEOUT_SECONDS = 60;

    public AntivirusScanResultDto scan(Path filePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "clamscan",
                "--no-summary",
                filePath.toAbsolutePath().toString()
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Process process = null;

        try {
            Process startedProcess = processBuilder.start();
            process = startedProcess;

            Future<String> stdoutFuture = executorService.submit(() -> readStream(startedProcess.getInputStream()));
            Future<String> stderrFuture = executorService.submit(() -> readStream(startedProcess.getErrorStream()));

            boolean finished = startedProcess.waitFor(ANTIVIRUS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                startedProcess.destroyForcibly();
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.TIMEOUT,
                        "Nao foi possivel concluir a verificacao antivirus no tempo limite.",
                        null
                );
            }

            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);
            String rawOutput = buildRawOutput(stdout, stderr);
            int exitCode = startedProcess.exitValue();

            if (exitCode == 0) {
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.CLEAN,
                        "Arquivo verificado e limpo.",
                        rawOutput
                );
            }

            if (exitCode == 1) {
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.INFECTED,
                        "Arquivo bloqueado por seguranca: possivel ameaca detectada pelo antivirus.",
                        rawOutput
                );
            }

            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Nao foi possivel concluir a verificacao antivirus do arquivo.",
                    rawOutput
            );
        } catch (IOException ex) {
            if (isClamAvUnavailable(ex)) {
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.NOT_INSTALLED,
                        "Scanner antivirus indisponivel no momento.",
                        ex.getMessage()
                );
            }

            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Nao foi possivel executar a verificacao antivirus do arquivo.",
                    ex.getMessage()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Execucao da verificacao antivirus interrompida.",
                    ex.getMessage()
            );
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            executorService.shutdownNow();
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

    private boolean isClamAvUnavailable(IOException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("No such file") || message.contains("cannot find the file"));
    }

    private String buildRawOutput(String stdout, String stderr) {
        String normalizedStdout = stdout == null ? "" : stdout.trim();
        String normalizedStderr = stderr == null ? "" : stderr.trim();

        if (!normalizedStdout.isEmpty() && !normalizedStderr.isEmpty()) {
            return normalizedStdout + System.lineSeparator() + normalizedStderr;
        }

        if (!normalizedStdout.isEmpty()) {
            return normalizedStdout;
        }

        if (!normalizedStderr.isEmpty()) {
            return normalizedStderr;
        }

        return null;
    }
}
