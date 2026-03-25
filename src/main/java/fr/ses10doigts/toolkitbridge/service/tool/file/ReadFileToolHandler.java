package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.exception.ToolExecutionException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileResponse;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class ReadFileToolHandler extends AbstractFileToolHandler {

    public ReadFileToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read the content of a text file from the workspace.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("path", "Relative file path in workspace", true)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        String path = (String) arguments.get("path");
        Path file = workspaceService.resolveInCurrentAgentWorkspace(path);

        if (!Files.exists(file)) {
            throw new ToolExecutionException("File does not exist: " + path);
        }

        if (!Files.isRegularFile(file)) {
            throw new ToolExecutionException("Path is not a file: " + path);
        }

        long size = Files.size(file);
        if (size > MAX_FILE_SIZE) {
            throw new ToolExecutionException("File too large to read");
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);

        return ToolExecutionResult.builder()
                .error(false)
                .message("File read successfully")
                .file(FileResponse.builder()
                        .path(relativePath(file))
                        .content(new FileContentResponse(content, content.length()))
                        .build())
                .build();
    }
}