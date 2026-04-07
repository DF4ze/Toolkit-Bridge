package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

import java.util.Set;

public record AgentPolicyDefinition(
        Set<String> allowedTools,
        Set<AgentMemoryScope> accessibleMemoryScopes,
        boolean delegationAllowed,
        boolean webAccessAllowed,
        boolean sharedWorkspaceWriteAllowed,
        boolean scriptedToolExecutionAllowed
) {
    public AgentPolicyDefinition {
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        accessibleMemoryScopes = accessibleMemoryScopes == null ? Set.of() : Set.copyOf(accessibleMemoryScopes);
    }
}
