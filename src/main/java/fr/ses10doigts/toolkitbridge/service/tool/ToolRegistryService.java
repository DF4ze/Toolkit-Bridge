package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ToolRegistryService {

    private final Map<String, ToolHandler> handlers;

    public ToolRegistryService(List<ToolHandler> toolHandlers) {
        this.handlers = toolHandlers.stream()
                .collect(Collectors.toMap(ToolHandler::name, Function.identity()));
    }

    public List<ToolDefinition> getToolDefinitions() {
        return handlers.values().stream()
                .map(this::toDefinition)
                .toList();
    }

    public ToolHandler getRequiredHandler(String name) {
        ToolHandler handler = handlers.get(name);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return handler;
    }

    public Set<String> getToolNames() {
        return Set.copyOf(handlers.keySet());
    }

    private ToolDefinition toDefinition(ToolHandler handler) {
        return ToolDefinition.function(
                new ToolSpec(
                        handler.name(),
                        handler.description(),
                        handler.parametersSchema()
                )
        );
    }
}
