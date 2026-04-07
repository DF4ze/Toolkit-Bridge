package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;

import java.time.Instant;
import java.util.Map;

public record HumanInterventionRequest(
        String requestId,
        Instant createdAt,
        String traceId,
        String agentId,
        AgentSensitiveAction sensitiveAction,
        HumanInterventionKind kind,
        HumanInterventionStatus status,
        String summary,
        String detail,
        Map<String, Object> metadata
) {

    public HumanInterventionRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (sensitiveAction == null) {
            throw new IllegalArgumentException("sensitiveAction must not be null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        traceId = normalize(traceId);
        agentId = agentId.trim();
        summary = summary.trim();
        detail = normalize(detail);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public HumanInterventionRequest withStatus(HumanInterventionStatus updatedStatus) {
        return new HumanInterventionRequest(
                requestId,
                createdAt,
                traceId,
                agentId,
                sensitiveAction,
                kind,
                updatedStatus,
                summary,
                detail,
                metadata
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
