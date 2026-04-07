package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;

import java.util.Locale;
import java.util.Set;

public record ResolvedAgentPolicy(
        String name,
        Set<String> allowedTools,
        Set<AgentMemoryScope> accessibleMemoryScopes,
        boolean delegationAllowed,
        boolean webAccessAllowed,
        boolean sharedWorkspaceWriteAllowed,
        boolean scriptedToolExecutionAllowed
) {
    public ResolvedAgentPolicy {
        name = normalize(name);
        allowedTools = allowedTools == null ? Set.of() : allowedTools.stream()
                .map(ResolvedAgentPolicy::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        accessibleMemoryScopes = accessibleMemoryScopes == null ? Set.of() : Set.copyOf(accessibleMemoryScopes);
    }

    public boolean allowsAnyTool() {
        return !allowedTools.isEmpty();
    }

    public boolean allowsTool(String toolName) {
        return allowedTools.contains(normalize(toolName));
    }

    public boolean allowsMemoryScope(AgentMemoryScope scope) {
        return scope != null && accessibleMemoryScopes.contains(scope);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
