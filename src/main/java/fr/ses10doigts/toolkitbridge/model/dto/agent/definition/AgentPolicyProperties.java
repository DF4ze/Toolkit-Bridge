package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Data
public class AgentPolicyProperties {

    private Set<String> allowedTools = new LinkedHashSet<>();
    private Set<String> accessibleMemoryScopes = new LinkedHashSet<>();
    private Boolean delegationAllowed = true;
    private Boolean webAccessAllowed = true;
    private Boolean sharedWorkspaceWriteAllowed = true;
    private Boolean scriptedToolExecutionAllowed = true;

    public AgentPolicyDefinition toDefinition() {
        return new AgentPolicyDefinition(
                normalizeTools(allowedTools),
                normalizeMemoryScopes(accessibleMemoryScopes),
                isEnabled(delegationAllowed),
                isEnabled(webAccessAllowed),
                isEnabled(sharedWorkspaceWriteAllowed),
                isEnabled(scriptedToolExecutionAllowed)
        );
    }

    private Set<String> normalizeTools(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<AgentMemoryScope> normalizeMemoryScopes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> AgentMemoryScope.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isEnabled(Boolean value) {
        return value == null || value;
    }
}
