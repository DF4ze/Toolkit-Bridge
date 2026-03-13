package fr.ses10doigts.toolkitbridge.service.botservice;


import fr.ses10doigts.toolkitbridge.model.dto.web.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.SimpleResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotFileService {

    private static final int MAX_FILE_SIZE = 1_000_000; // 1 MB lecture max

    private final WorkspaceService workspaceService;

    private void validateTextContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        if (content.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Content too large");
        }
    }

    /**
     * Liste le contenu d'un dossier du workspace.
     * path = "." ou "subdir"
     */
    public List<FileEntryResponse> listFiles(String path) throws IOException {
        String effectivePath = (path == null || path.isBlank()) ? "." : path;
        Path dir = workspaceService.resolveInCurrentBotWorkspace(effectivePath);

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
                                    workspaceService.relativizeFromCurrentBotWorkspace(p),
                                    Files.isDirectory(p),
                                    Files.isDirectory(p) ? 0L : Files.size(p)
                            );
                        } catch (IOException e) {
                            return new FileEntryResponse(
                                    p.getFileName().toString(),
                                    Files.isDirectory(p),
                                    -1L
                            );
                        }
                    })
                    .toList();
        }
    }

    public FileContentResponse readFile(String path) throws IOException {
        Path file = workspaceService.resolveInCurrentBotWorkspace(path);

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
                workspaceService.relativizeFromCurrentBotWorkspace(file),
                content
        );
    }

    public SimpleResponse writeFile(String path, String content) throws IOException {
        validateTextContent(content);

        Path file = workspaceService.resolveInCurrentBotWorkspace(path);
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

        return new SimpleResponse(
                false,
                "File written successfully: " + workspaceService.relativizeFromCurrentBotWorkspace(file)
        );
    }

    public SimpleResponse appendFile(String path, String content) throws IOException {
        validateTextContent(content);

        Path file = workspaceService.resolveInCurrentBotWorkspace(path);
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

        return new SimpleResponse(
                false,
                "File appended successfully: " + workspaceService.relativizeFromCurrentBotWorkspace(file)
        );
    }

    public SimpleResponse deleteFile(String path) throws IOException {
        Path file = workspaceService.resolveInCurrentBotWorkspace(path);

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
        Path source = workspaceService.resolveInCurrentBotWorkspace(sourcePath);
        Path target = workspaceService.resolveInCurrentBotWorkspace(targetPath);

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
                "Moved successfully from "
                        + workspaceService.relativizeFromCurrentBotWorkspace(source)
                        + " to "
                        + workspaceService.relativizeFromCurrentBotWorkspace(target)
        );
    }
}
