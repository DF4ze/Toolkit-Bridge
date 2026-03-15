package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileResponse;
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
    public ToolExecutionResult listFiles(String path) throws IOException {
        String effectivePath = (path == null || path.isBlank()) ? "." : path;
        Path dir = workspaceService.resolveInCurrentBotWorkspace(effectivePath);

        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + effectivePath);
        }

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path is not a directory: " + effectivePath);
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<FileEntryResponse> entryResponses = stream
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

            return ToolExecutionResult.builder()
                    .error(false)
                    .message("Folder listed successfully")
                    .file(FileResponse.builder()
                            .path(workspaceService.relativizeFromCurrentBotWorkspace(dir))
                            .entry(entryResponses)
                            .build())
                    .build();
        }
    }

    public ToolExecutionResult readFile(String path) throws IOException {
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

        FileContentResponse fcr = new FileContentResponse(
                content,
                content.length()
        );

        return ToolExecutionResult.builder()
                .error(false)
                .message("File read successfully")
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(file))
                        .content(fcr)
                        .build())
                .build();
    }

    public ToolExecutionResult writeFile(String path, String content) throws IOException {
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

        return ToolExecutionResult.builder()
                .error(false)
                .message("File written successfully: " + workspaceService.relativizeFromCurrentBotWorkspace(file))
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(file))
                        .build())
                .build();
    }

    public ToolExecutionResult appendFile(String path, String content) throws IOException {
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

        return ToolExecutionResult.builder()
                .error(false)
                .message("File appended successfully: " + workspaceService.relativizeFromCurrentBotWorkspace(file))
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(file))
                        .build())
                .build();
    }

    public ToolExecutionResult deleteFile(String path) throws IOException {
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

        return ToolExecutionResult.builder()
                .error(false)
                .message("File deleted successfully: " + workspaceService.relativizeFromCurrentBotWorkspace(file))
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(file))
                        .build())
                .build();
    }

    public ToolExecutionResult moveFile(String sourcePath, String targetPath) throws IOException {
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

        return ToolExecutionResult.builder()
                .error(false)
                .message("Moved successfully from "
                        + workspaceService.relativizeFromCurrentBotWorkspace(source)
                        + " to "
                        + workspaceService.relativizeFromCurrentBotWorkspace(target))
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(target))
                        .build())
                .build();
    }
}
