package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ToolRegistryService {

    private final Map<String, ToolHandler> handlers;

    public ToolRegistryService(List<ToolHandler> toolHandlers) {
        this.handlers = toolHandlers.stream()
                .collect(Collectors.toMap(ToolHandler::name, Function.identity()));
    }

    public List<OllamaToolDefinition> getToolDefinitions() {
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

    private OllamaToolDefinition toDefinition(ToolHandler handler) {
        return OllamaToolDefinition.function(
                new OllamaToolSpec(
                        handler.name(),
                        handler.description(),
                        handler.parametersSchema()
                )
        );
    }
}