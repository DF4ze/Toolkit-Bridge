package fr.ses10doigts.toolkitbridge.service;


import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class BotWorkspaceService {

    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final int MAX_FILE_SIZE = 1_000_000; // 1 MB lecture max
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Workspace dédié au bot.
     * A adapter selon ton environnement.
     */
    private static final Path BOT_WORKDIR = Path.of("~/workspace/bot").normalize();


    public BotWorkspaceService() throws IOException {
        Files.createDirectories(BOT_WORKDIR);
    }

    /**
     * Résout un chemin utilisateur à l'intérieur du workspace du bot.
     * Refuse les chemins absolus et toute tentative de sortie du dossier racine.
     */
    private Path resolveInWorkdir(String userPath) {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);

        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path resolved = BOT_WORKDIR.resolve(inputPath).normalize();

        if (!resolved.startsWith(BOT_WORKDIR)) {
            throw new ForbiddenCommandException("Path escapes bot workdir");
        }

        return resolved;
    }

    private String relativizeFromWorkdir(Path path) {
        return BOT_WORKDIR.relativize(path).toString().replace("\\", "/");
    }

    private void validateTextContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        if (content.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Content too large");
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                int remaining = MAX_OUTPUT_CHARS - sb.length();
                if (remaining <= 0) {
                    break;
                }
                sb.append(buffer, 0, Math.min(read, remaining));
            }
        }

        if (sb.length() >= MAX_OUTPUT_CHARS) {
            sb.append("\n[output truncated]");
        }

        return sb.toString();
    }

    private String safeGet(Future<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private void validateRequest(CommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getTool() == null || request.getTool().isBlank()) {
            throw new IllegalArgumentException("Tool cannot be empty");
        }

        if (request.getTimeout() != null && (request.getTimeout() < 1 || request.getTimeout() > 60)) {
            throw new IllegalArgumentException("Timeout must be between 1 and 60 seconds");
        }
    }

    private void validateArg(String arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        if (arg.length() > 300) {
            throw new IllegalArgumentException("Argument too long");
        }

        if (arg.matches(".*[\\r\\n\\x00].*")) {
            throw new IllegalArgumentException("Invalid control characters in argument");
        }

        if (!arg.matches("[a-zA-Z0-9._/\\-:=,@+ ]*")) {
            throw new IllegalArgumentException("Forbidden characters in argument: " + arg);
        }

        if (arg.contains("..")) {
            throw new ForbiddenCommandException("Parent path '..' is not allowed");
        }
    }


    /**
     * Liste le contenu d'un dossier du workspace.
     * path = "." ou "subdir"
     */
    public List<FileEntryResponse> listFiles(String path) throws IOException {
        String effectivePath = (path == null || path.isBlank()) ? "." : path;
        Path dir = resolveInWorkdir(effectivePath);

        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + effectivePath);
        }

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + effectivePath);
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .map(p -> {
                        try {
                            return new FileEntryResponse(
                                    relativizeFromWorkdir(p),
                                    Files.isDirectory(p),
                                    Files.isDirectory(p) ? 0L : Files.size(p)
                            );
                        } catch (IOException e) {
                            return new FileEntryResponse(
                                    relativizeFromWorkdir(p),
                                    Files.isDirectory(p),
                                    -1L
                            );
                        }
                    })
                    .toList();
        }
    }

    public FileContentResponse readFile(String path) throws IOException {
        Path file = resolveInWorkdir(path);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path is not a file: " + path);
        }

        long size = Files.size(file);
        if (size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large to read");
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);

        return new FileContentResponse(
                false,
                "File read successfully",
                relativizeFromWorkdir(file),
                content
        );
    }

    public SimpleResponse writeFile(String path, String content) throws IOException {
        validateTextContent(content);

        Path file = resolveInWorkdir(path);
        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return new SimpleResponse(false, "File written successfully: " + relativizeFromWorkdir(file));
    }

    public SimpleResponse appendFile(String path, String content) throws IOException {
        validateTextContent(content);

        Path file = resolveInWorkdir(path);
        Path parent = file.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        return new SimpleResponse(false, "File appended successfully: " + relativizeFromWorkdir(file));
    }

    public SimpleResponse deleteFile(String path) throws IOException {
        Path file = resolveInWorkdir(path);

        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }

        if (Files.isDirectory(file)) {
            try (Stream<Path> walk = Files.walk(file)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to delete: " + p, e);
                            }
                        });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException ioException) {
                    throw ioException;
                }
                throw e;
            }
        } else {
            Files.delete(file);
        }

        return new SimpleResponse(false, "Deleted successfully: " + path);
    }

    public SimpleResponse moveFile(String sourcePath, String targetPath) throws IOException {
        Path source = resolveInWorkdir(sourcePath);
        Path target = resolveInWorkdir(targetPath);

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source does not exist: " + sourcePath);
        }

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        return new SimpleResponse(
                false,
                "Moved successfully from " + relativizeFromWorkdir(source) + " to " + relativizeFromWorkdir(target)
        );
    }
}
