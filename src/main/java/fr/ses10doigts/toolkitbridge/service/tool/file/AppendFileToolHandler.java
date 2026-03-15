package fr.ses10doigts.toolkitbridge.service.tool.file;


import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Component
public class AppendFileToolHandler extends AbstractFileToolHandler {

    public AppendFileToolHandler(WorkspaceService workspaceService) {
        super(workspaceService);
    }

    @Override
    public String name() {
        return "append_file";
    }

    @Override
    public String description() {
        return "Append text to a file in the workspace, creating it if needed.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .stringProperty("path", "Relative file path in workspace", true)
                .stringProperty("content", "Text content to append", true)
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws IOException {
        return writeToFile(arguments, StandardOpenOption.APPEND);
    }
}