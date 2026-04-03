package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;

import java.util.Objects;

public record ResolvedRecipient(
        String agentId,
        AgentRole role,
        AgentRuntime runtime
) {
    public ResolvedRecipient {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        Objects.requireNonNull(runtime, "runtime must not be null");
    }
}

