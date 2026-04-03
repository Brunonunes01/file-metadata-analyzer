package com.metascan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metascan.dto.osint.UsernameScanProfileDto;
import com.metascan.dto.osint.UsernameScanResponseDto;
import com.metascan.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UsernameScanService {

    private static final int USERNAME_SCAN_TIMEOUT_SECONDS = 120;
    private static final String MAIGRET_DEFAULT_DB = "/usr/local/lib/python3.10/dist-packages/maigret/resources/data.json";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{2,64}$");

    private final SecureExecutionService secureExecutionService;
    private final ObjectMapper objectMapper;

    public UsernameScanService(
            SecureExecutionService secureExecutionService,
            ObjectMapper objectMapper
    ) {
        this.secureExecutionService = secureExecutionService;
        this.objectMapper = objectMapper;
    }

    public UsernameScanResponseDto scanUsername(String rawUsername) {
        String username = normalizeUsername(rawUsername);
        Path workDir = null;
        Path reportPath = null;
        Path writableDbPath = null;

        try {
            workDir = Files.createTempDirectory("metascan-maigret-");
            reportPath = resolveReportPath(workDir, username);
            writableDbPath = resolveWritableDbPath(workDir);
            prepareWritableDb(writableDbPath);

            List<String> commandArgs = List.of(
                    "maigret",
                    username,
                    "--db",
                    writableDbPath.toString(),
                    "--json",
                    "simple",
                    "--no-progressbar",
                    "--no-color",
                    "--folderoutput",
                    workDir.toString()
            );

            SecureExecutionService.CommandResult commandResult = secureExecutionService.execute(
                    commandArgs,
                    USERNAME_SCAN_TIMEOUT_SECONDS
            );

            if (commandResult.timedOut()) {
                throw new BadRequestException("Tempo limite excedido ao executar o scan de username.");
            }

            String stderr = commandResult.stderr() == null ? "" : commandResult.stderr().toLowerCase(Locale.ROOT);
            if (stderr.contains("not found") || stderr.contains("no such file")) {
                throw new BadRequestException("Maigret nao esta instalado no ambiente do backend.");
            }

            JsonNode root = readMaigretOutput(reportPath);
            List<UsernameScanProfileDto> profiles = extractProfiles(root);
            profiles.sort(Comparator.comparing(UsernameScanProfileDto::platform, String.CASE_INSENSITIVE_ORDER));
            String message = buildResultMessage(profiles, commandResult.stdout());

            return new UsernameScanResponseDto(
                    username,
                    Instant.now(),
                    "completed",
                    message,
                    profiles.size(),
                    profiles
            );
        } catch (IOException ex) {
            throw new BadRequestException("Falha ao executar o scan de username.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Execucao do scan de username interrompida.");
        } finally {
            deleteQuietly(reportPath);
            deleteQuietly(writableDbPath);
            deleteQuietly(workDir);
        }
    }

    private Path resolveReportPath(Path workDir, String username) {
        return workDir.resolve("report_" + username + "_simple.json");
    }

    private Path resolveWritableDbPath(Path workDir) {
        return workDir.resolve("maigret-db.json");
    }

    private void prepareWritableDb(Path writableDbPath) throws IOException {
        Path defaultDbPath = Paths.get(MAIGRET_DEFAULT_DB);
        if (!Files.exists(defaultDbPath)) {
            throw new BadRequestException("Base de dados do Maigret nao encontrada no ambiente.");
        }

        Files.copy(defaultDbPath, writableDbPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private JsonNode readMaigretOutput(Path reportPath) throws IOException {
        if (reportPath != null && Files.exists(reportPath) && Files.size(reportPath) > 0) {
            return objectMapper.readTree(Files.readString(reportPath));
        }
        throw new BadRequestException("Maigret nao retornou dados de scan.");
    }

    private List<UsernameScanProfileDto> extractProfiles(JsonNode root) {
        List<UsernameScanProfileDto> profiles = new ArrayList<>();

        if (root == null || root.isNull()) {
            return profiles;
        }

        if (root.isObject() && !root.has("sites")) {
            root.fields().forEachRemaining(entry -> addProfileIfFound(profiles, entry.getKey(), entry.getValue()));
        }

        JsonNode sitesNode = root.path("sites");
        if (sitesNode.isObject()) {
            sitesNode.fields().forEachRemaining(entry -> addProfileIfFound(profiles, entry.getKey(), entry.getValue()));
        }

        JsonNode resultsNode = root.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode resultNode : resultsNode) {
                String platform = firstNonBlank(
                        textOrNull(resultNode.path("site")),
                        textOrNull(resultNode.path("name")),
                        "unknown"
                );
                addProfileIfFound(profiles, platform, resultNode);
            }
        }

        return profiles;
    }

    private void addProfileIfFound(List<UsernameScanProfileDto> profiles, String platform, JsonNode siteNode) {
        String normalizedStatus = normalizeStatus(siteNode);
        if (!"found".equals(normalizedStatus)) {
            return;
        }

        String profileUrl = firstNonBlank(
                textOrNull(siteNode.path("url_user")),
                textOrNull(siteNode.path("status").path("url")),
                textOrNull(siteNode.path("url")),
                textOrNull(siteNode.path("profile_url")),
                textOrNull(siteNode.path("link")),
                ""
        );

        if (profileUrl.isBlank()) {
            return;
        }

        profiles.add(new UsernameScanProfileDto(platform, profileUrl, normalizedStatus));
    }

    private String normalizeStatus(JsonNode siteNode) {
        String status = textOrNull(siteNode.path("status").path("status"));
        if (status == null) {
            status = textOrNull(siteNode.path("status"));
        }

        if (status != null) {
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("claim") || normalized.contains("found") || normalized.contains("exist")) {
                return "found";
            }
            if (normalized.contains("available") || normalized.contains("not found")) {
                return "not_found";
            }
        }

        JsonNode idsNode = siteNode.path("ids");
        if (idsNode.isArray() && !idsNode.isEmpty()) {
            return "found";
        }

        JsonNode existsNode = siteNode.path("exists");
        if (existsNode.isBoolean()) {
            return existsNode.booleanValue() ? "found" : "not_found";
        }

        return "unknown";
    }

    private String normalizeUsername(String rawUsername) {
        if (rawUsername == null || rawUsername.isBlank()) {
            throw new BadRequestException("Username obrigatorio para scan.");
        }

        String username = rawUsername.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BadRequestException("Username invalido. Use apenas letras, numeros, '.', '_' e '-'.");
        }

        return username;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String buildResultMessage(List<UsernameScanProfileDto> profiles, String commandStdout) {
        if (profiles != null && !profiles.isEmpty()) {
            return "Scan concluido com sucesso.";
        }

        String output = commandStdout == null ? "" : commandStdout.toLowerCase(Locale.ROOT);
        if (output.contains("too many errors") || output.contains("request timeout") || output.contains("connecting failure")) {
            return "Scan concluido, mas com baixa confiabilidade por falhas de rede nos provedores consultados.";
        }

        return "Nenhum perfil publico encontrado para este username.";
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
