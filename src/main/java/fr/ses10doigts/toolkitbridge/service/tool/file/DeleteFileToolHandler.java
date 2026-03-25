package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.exception.ToolExecutionException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class DeleteFileToolHandler extends AbstractFileToolHandler {

    public DeleteFileToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "delete_file";
    }

    @Override
    public String description() {
        return "Delete a file or directory from the workspace.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("path", "Relative file or directory path in workspace", true)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        String path = (String) arguments.get("path");
        Path target = workspaceService.resolveInCurrentAgentWorkspace(path);

        if (!Files.exists(target)) {
            throw new ToolExecutionException("Path does not exist: " + path);
        }

        String relativeTarget = relativePath(target);

        if (Files.isDirectory(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
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
            Files.delete(target);
        }

        return successWithPath("File deleted successfully: " + relativeTarget, target);
    }
}