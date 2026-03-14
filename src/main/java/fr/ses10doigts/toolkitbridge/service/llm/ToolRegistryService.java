package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ToolRegistryService {

    private final List<OllamaToolDefinition> toolDefinitions;

    public ToolRegistryService() {
        this.toolDefinitions = List.of(
                fileListTool(),
                readFileTool(),
                writeFileTool(),
                appendFileTool(),
                moveFileTool(),
                deleteFileTool(),
                runCommandTool()
        );
    }

    public List<OllamaToolDefinition> listTools() {
        return toolDefinitions;
    }

    public Optional<OllamaToolDefinition> findByName(String toolName) {
        return toolDefinitions.stream()
                .filter(definition -> definition.function().name().equals(toolName))
                .findFirst();
    }

    private OllamaToolDefinition fileListTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "list_files",
                "List files and folders in a workspace directory.",
                objectSchema(
                        Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative workspace directory path. Use '.' for the root."
                                )
                        ),
                        List.of()
                )
        ));
    }

    private OllamaToolDefinition readFileTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "read_file",
                "Read a UTF-8 text file from the workspace.",
                objectSchema(
                        Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative workspace file path to read."
                                )
                        ),
                        List.of("path")
                )
        ));
    }

    private OllamaToolDefinition writeFileTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "write_file",
                "Create or overwrite a UTF-8 text file in the workspace.",
                objectSchema(
                        Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative workspace file path to write."
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "Full file content to write."
                                )
                        ),
                        List.of("path", "content")
                )
        ));
    }

    private OllamaToolDefinition appendFileTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "append_file",
                "Append UTF-8 text content to a workspace file.",
                objectSchema(
                        Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative workspace file path to append to."
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "Text to append to the target file."
                                )
                        ),
                        List.of("path", "content")
                )
        ));
    }

    private OllamaToolDefinition moveFileTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "move_file",
                "Move or rename a file or directory in the workspace.",
                objectSchema(
                        Map.of(
                                "sourcePath", Map.of(
                                        "type", "string",
                                        "description", "Relative source path in the workspace."
                                ),
                                "targetPath", Map.of(
                                        "type", "string",
                                        "description", "Relative destination path in the workspace."
                                )
                        ),
                        List.of("sourcePath", "targetPath")
                )
        ));
    }

    private OllamaToolDefinition deleteFileTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "delete_file",
                "Delete a file or directory recursively from the workspace.",
                objectSchema(
                        Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "Relative workspace path to delete."
                                )
                        ),
                        List.of("path")
                )
        ));
    }

    private OllamaToolDefinition runCommandTool() {
        return OllamaToolDefinition.function(new OllamaToolSpec(
                "run_command",
                "Execute a safe allow-listed shell tool in the workspace.",
                objectSchema(
                        Map.of(
                                "tool", Map.of(
                                        "type", "string",
                                        "description", "Allowed command name (for example: ls, pwd, cat, grep, find)."
                                ),
                                "args", Map.of(
                                        "type", "array",
                                        "description", "Command arguments.",
                                        "items", Map.of("type", "string")
                                ),
                                "timeout", Map.of(
                                        "type", "integer",
                                        "minimum", 1,
                                        "maximum", 60,
                                        "description", "Timeout in seconds."
                                )
                        ),
                        List.of("tool")
                )
        ));
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
