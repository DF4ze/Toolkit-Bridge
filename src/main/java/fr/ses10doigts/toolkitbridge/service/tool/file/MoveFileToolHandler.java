package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.exception.ToolExecutionException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Component
public class MoveFileToolHandler extends AbstractFileToolHandler {

    public MoveFileToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "move_file";
    }

    @Override
    public String description() {
        return "Move or rename a file or directory inside the workspace.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("source_path", "Current relative source path", true)
                .stringProperty("target_path", "New relative target path", true)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        String sourcePath = (String) arguments.get("source_path");
        String targetPath = (String) arguments.get("target_path");

        Path source = workspaceService.resolveInCurrentAgentWorkspace(sourcePath);
        Path target = workspaceService.resolveInCurrentAgentWorkspace(targetPath);

        if (!Files.exists(source)) {
            throw new ToolExecutionException("Source does not exist: " + sourcePath);
        }

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

        return successWithPath(
                "Moved successfully from " + relativePath(source) + " to " + relativePath(target),
                target
        );
    }
}