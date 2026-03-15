package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.exception.ToolValidationException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolHandler;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@RequiredArgsConstructor
public abstract class AbstractFileToolHandler implements ToolHandler {

    protected static final int MAX_FILE_SIZE = 1_000_000;

    protected final WorkspaceService workspaceService;

    protected void validateTextContent(String content) {
        if (content == null) {
            throw new ToolValidationException("Content cannot be null");
        }

        if (content.length() > MAX_FILE_SIZE) {
            throw new ToolValidationException("Content too large");
        }
    }

    protected ToolExecutionResult successWithPath(String message, Path path) throws IOException {
        return ToolExecutionResult.builder()
                .error(false)
                .message(message)
                .file(FileResponse.builder()
                        .path(workspaceService.relativizeFromCurrentBotWorkspace(path))
                        .build())
                .build();
    }

    protected String relativePath(Path path) throws IOException {
        return workspaceService.relativizeFromCurrentBotWorkspace(path);
    }

    public ToolExecutionResult writeToFile(Map<String, Object> arguments, StandardOpenOption option) throws IOException {
        String path = (String) arguments.get("path");
        String content = (String) arguments.get("content");

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
                option
        );

        return successWithPath("File "+
                (option == StandardOpenOption.APPEND ? "appended" : "written")+
                " successfully: " + relativePath(file), file);
    }
}