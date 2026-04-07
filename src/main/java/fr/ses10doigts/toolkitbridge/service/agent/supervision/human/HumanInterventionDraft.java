package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;

import java.util.Map;

public record HumanInterventionDraft(
        String traceId,
        String agentId,
        AgentSensitiveAction sensitiveAction,
        HumanInterventionKind kind,
        String summary,
        String detail,
        Map<String, Object> metadata
) {

    public HumanInterventionDraft {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (sensitiveAction == null) {
            throw new IllegalArgumentException("sensitiveAction must not be null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        traceId = normalize(traceId);
        agentId = agentId.trim();
        summary = summary.trim();
        detail = normalize(detail);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
