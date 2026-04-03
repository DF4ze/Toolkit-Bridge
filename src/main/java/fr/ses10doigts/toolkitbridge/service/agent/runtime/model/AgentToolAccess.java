package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import java.util.Set;

public record AgentToolAccess(
        boolean enabled,
        Set<String> allowedTools
) {
    public AgentToolAccess {
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
    }
}
