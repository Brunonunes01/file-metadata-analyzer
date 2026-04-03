package com.metascan.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class SecureExecutionService {

    public CommandResult execute(List<String> commandArgs, int timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Process process = null;

        try {
            Process startedProcess = processBuilder.start();
            process = startedProcess;

            Future<String> stdoutFuture = executorService.submit(() -> readStream(startedProcess.getInputStream()));
            Future<String> stderrFuture = executorService.submit(() -> readStream(startedProcess.getErrorStream()));

            boolean finished = startedProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                startedProcess.destroyForcibly();
                return new CommandResult(-1, "", "", true);
            }

            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);
            int exitCode = startedProcess.exitValue();

            return new CommandResult(exitCode, stdout, stderr, false);
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

    public record CommandResult(
            int exitCode,
            String stdout,
            String stderr,
            boolean timedOut
    ) {
    }
}
