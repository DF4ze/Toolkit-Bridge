package fr.ses10doigts.toolkitbridge.service.tool.memory;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitFactMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.service.ExplicitMemoryToolService;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class WriteMemoryFactToolHandler extends AbstractMemoryToolHandler {

    public static final String NAME = "write_memory_fact";

    private final ExplicitMemoryToolService memoryToolService;
    private final CurrentAgentService currentAgentService;

    public WriteMemoryFactToolHandler(
            ExplicitMemoryToolService memoryToolService,
            CurrentAgentService currentAgentService
    ) {
        this.memoryToolService = memoryToolService;
        this.currentAgentService = currentAgentService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Create or update an explicit memory fact for the current agent with a controlled scope.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("content"),
                "properties", Map.of(
                        "memory_id", Map.of("type", "integer"),
                        "user_id", Map.of("type", "string"),
                        "project_id", Map.of("type", "string"),
                        "scope", Map.of("type", "string", "enum", List.of("AGENT", "USER", "PROJECT")),
                        "scope_id", Map.of("type", "string"),
                        "type", Map.of("type", "string", "enum", List.of("FACT", "PREFERENCE", "DECISION", "CONTEXT")),
                        "content", Map.of("type", "string"),
                        "importance", Map.of("type", "number"),
                        "tags", Map.of("type", "array", "items", Map.of("type", "string"))
                )
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        Long memoryId = asLong(arguments.get("memory_id"));
        MemoryEntry entry = memoryToolService.writeFact(new ExplicitFactMemoryWriteRequest(
                memoryId,
                currentAgentId(),
                asString(arguments.get("user_id")),
                asString(arguments.get("project_id")),
                asMemoryScope(arguments.get("scope")),
                asString(arguments.get("scope_id")),
                asMemoryType(arguments.get("type")),
                asString(arguments.get("content")),
                asDouble(arguments.get("importance")),
                asStringSet(arguments.get("tags"))
        ));

        String verb = memoryId == null ? "created" : "updated";
        return successWithFact("Memory fact " + verb + " successfully.", NAME, entry);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        return switch (value) {
            case null -> null;
            case Long longValue -> longValue;
            case Integer integer -> integer.longValue();
            default -> Long.parseLong(String.valueOf(value));
        };
    }

    private Double asDouble(Object value) {
        return switch (value) {
            case null -> null;
            case Double doubleValue -> doubleValue;
            case Number number -> number.doubleValue();
            default -> Double.parseDouble(String.valueOf(value));
        };
    }

    private Set<String> asStringSet(Object value) {
        if (!(value instanceof List<?> items)) {
            return null;
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object item : items) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    private MemoryScope asMemoryScope(Object value) {
        return value == null ? null : MemoryScope.valueOf(String.valueOf(value));
    }

    private MemoryType asMemoryType(Object value) {
        return value == null ? null : MemoryType.valueOf(String.valueOf(value));
    }

    private String currentAgentId() {
        return currentAgentService.getCurrentAgent().agentIdent();
    }
}
