package fr.ses10doigts.toolkitbridge.service.agent.communication.model;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;

import java.util.Objects;

public record AgentRecipient(
        AgentRecipientKind kind,
        String agentId,
        AgentRole role
) {

    public AgentRecipient {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind == AgentRecipientKind.AGENT && (agentId == null || agentId.isBlank())) {
            throw new IllegalArgumentException("agentId must not be blank when kind is AGENT");
        }
        if (kind == AgentRecipientKind.AGENT && role != null) {
            throw new IllegalArgumentException("role must be null when kind is AGENT");
        }
        if (kind == AgentRecipientKind.ROLE && role == null) {
            throw new IllegalArgumentException("role must not be null when kind is ROLE");
        }
        if (kind == AgentRecipientKind.ROLE && agentId != null && !agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must be null when kind is ROLE");
        }
    }

    public static AgentRecipient forAgent(String agentId) {
        return new AgentRecipient(AgentRecipientKind.AGENT, agentId, null);
    }

    public static AgentRecipient forRole(AgentRole role) {
        return new AgentRecipient(AgentRecipientKind.ROLE, null, role);
    }
}

