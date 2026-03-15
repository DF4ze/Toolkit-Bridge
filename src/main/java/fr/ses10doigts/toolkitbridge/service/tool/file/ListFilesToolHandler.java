package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.exception.ToolExecutionException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class ListFilesToolHandler extends AbstractFileToolHandler {

    public ListFilesToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String description() {
        return "List files and directories in a workspace directory.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("path", "Relative directory path in workspace. Use '.' for root.", false)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        String path = (String) arguments.get("path");
        String effectivePath = (path == null || path.isBlank()) ? "." : path;

        Path dir = workspaceService.resolveInCurrentBotWorkspace(effectivePath);

        if (!Files.exists(dir)) {
            throw new ToolExecutionException("Directory does not exist: " + effectivePath);
        }

        if (!Files.isDirectory(dir)) {
            throw new ToolExecutionException("Path is not a directory: " + effectivePath);
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<FileEntryResponse> entries = stream
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .map(p -> {
                        try {
                            return new FileEntryResponse(
                                    relativePath(p),
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
                            .path(relativePath(dir))
                            .entry(entries)
                            .build())
                    .build();
        }
    }
}