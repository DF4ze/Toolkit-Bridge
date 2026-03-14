package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.LlmToolDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ToolRegistryService {

    private final List<LlmToolDefinition> tools = List.of(
            listFilesTool(),
            readFileTool(),
            writeFileTool(),
            appendFileTool(),
            moveFileTool(),
            deleteFileTool(),
            runCommandTool()
    );

    public List<LlmToolDefinition> listTools() {
        return tools;
    }

    public Optional<LlmToolDefinition> getTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }

        return tools.stream()
                .filter(tool -> tool.name().equals(toolName))
                .findFirst();
    }

    private LlmToolDefinition listFilesTool() {
        return new LlmToolDefinition(
                "list_files",
                "List files and folders from a directory in the bot workspace.",
                objectSchema(
                        Map.of(
                                "path", stringProperty("Directory path relative to workspace root. Defaults to '.'.")
                        ),
                        List.of()
                )
        );
    }

    private LlmToolDefinition readFileTool() {
        return new LlmToolDefinition(
                "read_file",
                "Read the content of a UTF-8 text file from the workspace.",
                objectSchema(
                        Map.of(
                                "path", stringProperty("File path relative to workspace root.")
                        ),
                        List.of("path")
                )
        );
    }

    private LlmToolDefinition writeFileTool() {
        return new LlmToolDefinition(
                "write_file",
                "Create or overwrite a UTF-8 text file in the workspace.",
                objectSchema(
                        Map.of(
                                "path", stringProperty("File path relative to workspace root."),
                                "content", stringProperty("Full text content to write.")
                        ),
                        List.of("path", "content")
                )
        );
    }

    private LlmToolDefinition appendFileTool() {
        return new LlmToolDefinition(
                "append_file",
                "Append UTF-8 text content at the end of a workspace file.",
                objectSchema(
                        Map.of(
                                "path", stringProperty("File path relative to workspace root."),
                                "content", stringProperty("Text to append.")
                        ),
                        List.of("path", "content")
                )
        );
    }

    private LlmToolDefinition moveFileTool() {
        return new LlmToolDefinition(
                "move_file",
                "Move or rename a file/directory in the workspace.",
                objectSchema(
                        Map.of(
                                "sourcePath", stringProperty("Source path relative to workspace root."),
                                "targetPath", stringProperty("Target path relative to workspace root.")
                        ),
                        List.of("sourcePath", "targetPath")
                )
        );
    }

    private LlmToolDefinition deleteFileTool() {
        return new LlmToolDefinition(
                "delete_file",
                "Delete a file or directory recursively in the workspace.",
                objectSchema(
                        Map.of(
                                "path", stringProperty("Path to delete relative to workspace root.")
                        ),
                        List.of("path")
                )
        );
    }

    private LlmToolDefinition runCommandTool() {
        return new LlmToolDefinition(
                "run_command",
                "Run an allow-listed shell command in workspace context.",
                objectSchema(
                        Map.of(
                                "tool", stringProperty("Allowed command binary name."),
                                "args", Map.of(
                                        "type", "array",
                                        "description", "Arguments passed to the selected command.",
                                        "items", Map.of("type", "string")
                                ),
                                "timeout", Map.of(
                                        "type", "integer",
                                        "minimum", 1,
                                        "maximum", 60,
                                        "description", "Execution timeout in seconds."
                                )
                        ),
                        List.of("tool")
                )
        );
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of(
                "type", "string",
                "description", description
        );
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false
        );
    }
}
