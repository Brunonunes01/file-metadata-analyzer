package com.metascan.service;

import com.metascan.dto.AntivirusScanResultDto;
import com.metascan.dto.AntivirusScanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class AntivirusScanService {

    private static final Logger log = LoggerFactory.getLogger(AntivirusScanService.class);
    private static final int ANTIVIRUS_TIMEOUT_SECONDS = 60;
    private static final int CLAMD_CHUNK_SIZE = 8192;
    private static final String MODE_CLAMSCAN = "clamscan";
    private static final String MODE_CLAMD = "clamd";

    private final String antivirusMode;
    private final String clamdHost;
    private final int clamdPort;
    private final SecureExecutionService secureExecutionService;

    public AntivirusScanService(
            @Value("${metascan.antivirus.mode:clamscan}") String antivirusMode,
            @Value("${metascan.antivirus.clamd.host:localhost}") String clamdHost,
            @Value("${metascan.antivirus.clamd.port:3310}") int clamdPort,
            SecureExecutionService secureExecutionService
    ) {
        this.antivirusMode = normalizeMode(antivirusMode);
        this.clamdHost = clamdHost;
        this.clamdPort = clamdPort;
        this.secureExecutionService = secureExecutionService;
    }

    public AntivirusScanResultDto scan(Path filePath) {
        if (MODE_CLAMD.equals(antivirusMode)) {
            return scanWithClamd(filePath);
        }
        return scanWithClamScan(filePath);
    }

    private AntivirusScanResultDto scanWithClamScan(Path filePath) {
        List<String> commandArgs = List.of(
                "clamscan",
                "--no-summary",
                filePath.toAbsolutePath().toString()
        );

        try {
            SecureExecutionService.CommandResult commandResult = secureExecutionService.execute(commandArgs, ANTIVIRUS_TIMEOUT_SECONDS);
            if (commandResult.timedOut()) {
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.TIMEOUT,
                        "Nao foi possivel concluir a verificacao antivirus no tempo limite.",
                        null
                );
            }

            String rawOutput = buildRawOutput(commandResult.stdout(), commandResult.stderr());
            int exitCode = commandResult.exitCode();

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
                log.warn("ClamAV indisponivel no host.");
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.NOT_INSTALLED,
                        "Scanner antivirus indisponivel no momento.",
                        ex.getMessage()
                );
            }

            log.warn("Falha ao executar ClamAV: {}", ex.getClass().getSimpleName());
            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Nao foi possivel executar a verificacao antivirus do arquivo.",
                    ex.getMessage()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Execucao do ClamAV interrompida.");
            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Execucao da verificacao antivirus interrompida.",
                    ex.getMessage()
            );
        }
    }

    private AntivirusScanResultDto scanWithClamd(Path filePath) {
        long timeoutMillis = TimeUnit.SECONDS.toMillis(ANTIVIRUS_TIMEOUT_SECONDS);
        long startMillis = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(clamdHost, clamdPort), (int) timeoutMillis);
            socket.setSoTimeout((int) timeoutMillis);

            try (DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                 InputStream responseStream = new BufferedInputStream(socket.getInputStream());
                 InputStream fileInputStream = new BufferedInputStream(Files.newInputStream(filePath))) {

                outputStream.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

                byte[] chunkBuffer = new byte[CLAMD_CHUNK_SIZE];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(chunkBuffer)) != -1) {
                    if (System.currentTimeMillis() - startMillis > timeoutMillis) {
                        return new AntivirusScanResultDto(
                                AntivirusScanStatus.TIMEOUT,
                                "Nao foi possivel concluir a verificacao antivirus no tempo limite.",
                                null
                        );
                    }
                    outputStream.write(ByteBuffer.allocate(4).putInt(bytesRead).array());
                    outputStream.write(chunkBuffer, 0, bytesRead);
                }

                outputStream.writeInt(0);
                outputStream.flush();

                String response = readClamdResponse(responseStream);
                if (response.isBlank()) {
                    return new AntivirusScanResultDto(
                            AntivirusScanStatus.SCAN_ERROR,
                            "Nao foi possivel concluir a verificacao antivirus do arquivo.",
                            null
                    );
                }

                if (response.contains(" FOUND")) {
                    return new AntivirusScanResultDto(
                            AntivirusScanStatus.INFECTED,
                            "Arquivo bloqueado por seguranca: possivel ameaca detectada pelo antivirus.",
                            response
                    );
                }

                if (response.endsWith("OK")) {
                    return new AntivirusScanResultDto(
                            AntivirusScanStatus.CLEAN,
                            "Arquivo verificado e limpo.",
                            response
                    );
                }

                return new AntivirusScanResultDto(
                        AntivirusScanStatus.SCAN_ERROR,
                        "Nao foi possivel concluir a verificacao antivirus do arquivo.",
                        response
                );
            }
        } catch (IOException ex) {
            if (isClamAvUnavailable(ex)) {
                log.warn("ClamAV daemon indisponivel em {}:{}.", clamdHost, clamdPort);
                return new AntivirusScanResultDto(
                        AntivirusScanStatus.NOT_INSTALLED,
                        "Scanner antivirus indisponivel no momento.",
                        ex.getMessage()
                );
            }
            log.warn("Falha ao executar ClamAV daemon: {}", ex.getClass().getSimpleName());
            return new AntivirusScanResultDto(
                    AntivirusScanStatus.SCAN_ERROR,
                    "Nao foi possivel executar a verificacao antivirus do arquivo.",
                    ex.getMessage()
            );
        }
    }

    private boolean isClamAvUnavailable(IOException ex) {
        String message = ex.getMessage();
        return message != null
                && (message.contains("No such file")
                || message.contains("cannot find the file")
                || message.contains("Connection refused")
                || message.contains("Connection timed out"));
    }

    private String normalizeMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return MODE_CLAMSCAN;
        }
        String normalized = rawMode.trim().toLowerCase(Locale.ROOT);
        if (MODE_CLAMD.equals(normalized)) {
            return MODE_CLAMD;
        }
        return MODE_CLAMSCAN;
    }

    private String readClamdResponse(InputStream inputStream) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        int nextByte;
        while ((nextByte = inputStream.read()) != -1) {
            if (nextByte == '\n' || nextByte == '\0') {
                break;
            }
            responseBuilder.append((char) nextByte);
        }
        return responseBuilder.toString().trim();
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
