package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Component
public class WriteFileToolHandler extends AbstractFileToolHandler {

    public WriteFileToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Create or replace a text file in the workspace.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("path", "Relative file path in workspace", true)
                .stringProperty("content", "Full text content to write", true)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        return writeToFile(arguments, StandardOpenOption.TRUNCATE_EXISTING);
    }
}