package fr.ses10doigts.toolkitbridge.service.tool.memory;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.tool.model.MemoryContextRecallRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.service.ExplicitMemoryToolService;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RecallMemoryContextToolHandler extends AbstractMemoryToolHandler {

    public static final String NAME = "recall_memory_context";

    private final ExplicitMemoryToolService memoryToolService;
    private final CurrentAgentService currentAgentService;

    public RecallMemoryContextToolHandler(
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
        return "Recall assembled memory context for the current agent within a specific conversation scope.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("conversation_id"),
                "properties", Map.of(
                        "conversation_id", Map.of("type", "string"),
                        "user_id", Map.of("type", "string"),
                        "project_id", Map.of("type", "string"),
                        "focus", Map.of("type", "string"),
                        "max_semantic_memories", Map.of("type", "integer"),
                        "max_episodes", Map.of("type", "integer")
                )
        );
    }

    @Override
    protected ToolCapability defaultMemoryCapability() {
        return ToolCapability.MEMORY_READ;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        MemoryContext context = memoryToolService.recallContext(new MemoryContextRecallRequest(
                currentAgentId(),
                asString(arguments.get("user_id")),
                asString(arguments.get("project_id")),
                asString(arguments.get("conversation_id")),
                asString(arguments.get("focus")),
                asInteger(arguments.get("max_semantic_memories")),
                asInteger(arguments.get("max_episodes"))
        ));

        return successWithContext("Memory context recalled successfully.", NAME, context.text());
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        return switch (value) {
            case null -> null;
            case Integer integer -> integer;
            case Long longValue -> Math.toIntExact(longValue);
            default -> Integer.parseInt(String.valueOf(value));
        };
    }

    private String currentAgentId() {
        return currentAgentService.getCurrentAgent().agentIdent();
    }
}
