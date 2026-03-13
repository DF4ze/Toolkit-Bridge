package fr.ses10doigts.toolkitbridge.service;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentBotService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Service
public class WorkspaceService {

    private static final int MAX_COMMAND_ARG_LENGTH = 300;
    private static final String ALLOWED_COMMAND_ARG_PATTERN = "[a-zA-Z0-9._/\\-:=,@+ ]*";

    @Getter
    private final Path botsRoot;
    private final CurrentBotService currentBotService;

    public WorkspaceService(
            @Value("${app.workspace.bots-root}") String botsRoot,
            CurrentBotService currentBotService
    ) throws IOException {
        this.botsRoot = Path.of(botsRoot).normalize();
        this.currentBotService = currentBotService;
        Files.createDirectories(this.botsRoot);
    }

    public Path getCurrentBotWorkspace() throws IOException {
        AuthenticatedBot bot = currentBotService.getCurrentBot();
        String safeBotFolderName = sanitizeBotFolderName(bot.botIdent());

        Path botWorkspace = botsRoot.resolve(safeBotFolderName).normalize();

        if (!botWorkspace.startsWith(botsRoot)) {
            throw new ForbiddenCommandException("Resolved bot workspace escapes bots root");
        }

        Files.createDirectories(botWorkspace);
        return botWorkspace;
    }

    public Path resolveInCurrentBotWorkspace(String userPath) throws IOException {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);

        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path botWorkspace = getCurrentBotWorkspace();
        Path resolved = botWorkspace.resolve(inputPath).normalize();

        if (!resolved.startsWith(botWorkspace)) {
            throw new ForbiddenCommandException("Path escapes bot workspace");
        }

        return resolved;
    }

    public String relativizeFromCurrentBotWorkspace(Path path) throws IOException {
        return getCurrentBotWorkspace()
                .relativize(path)
                .toString()
                .replace("\\", "/");
    }

    public void validateCommandArg(String arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        if (arg.length() > MAX_COMMAND_ARG_LENGTH) {
            throw new IllegalArgumentException("Argument too long");
        }

        if (arg.matches(".*[\\r\\n\\x00].*")) {
            throw new IllegalArgumentException("Invalid control characters in argument");
        }

        if (!arg.matches(ALLOWED_COMMAND_ARG_PATTERN)) {
            throw new IllegalArgumentException("Forbidden characters in argument: " + arg);
        }
    }

    public Path validateRelativeWorkspacePathArg(String arg) throws IOException {
        validateCommandArg(arg);
        return resolveInCurrentBotWorkspace(arg);
    }

    private String sanitizeBotFolderName(String botIdent) {
        if (botIdent == null || botIdent.isBlank()) {
            throw new IllegalArgumentException("Bot identifier cannot be empty");
        }

        String normalized = botIdent.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9._-]", "_");

        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Bot identifier produces an empty workspace folder name");
        }

        return sanitized;
    }
}
