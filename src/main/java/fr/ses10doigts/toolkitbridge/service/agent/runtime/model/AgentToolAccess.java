package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;

import java.util.List;
import java.util.Set;

public record AgentToolAccess(
        boolean enabled,
        Set<String> allowedTools,
        List<ToolDescriptor> registeredTools,
        List<ToolDescriptor> exposedTools
) {
    public AgentToolAccess(boolean enabled, Set<String> allowedTools) {
        this(enabled, allowedTools, List.of(), List.of());
    }

    public AgentToolAccess {
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        registeredTools = registeredTools == null ? List.of() : List.copyOf(registeredTools);
        exposedTools = exposedTools == null ? List.of() : List.copyOf(exposedTools);
    }

    public boolean hasExposedTools() {
        return enabled && !exposedTools.isEmpty();
    }

    public List<ToolDefinition> exposedToolDefinitions() {
        return exposedTools.stream()
                .map(ToolDescriptor::toToolDefinition)
                .toList();
    }
}
