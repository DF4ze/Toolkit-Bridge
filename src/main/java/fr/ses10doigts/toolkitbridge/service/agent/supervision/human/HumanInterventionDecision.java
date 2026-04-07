package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import java.time.Instant;
import java.util.Map;

public record HumanInterventionDecision(
        String requestId,
        HumanInterventionStatus status,
        Instant decidedAt,
        String actorId,
        String channel,
        String comment,
        Map<String, Object> metadata
) {

    public HumanInterventionDecision {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (status == null || status == HumanInterventionStatus.PENDING) {
            throw new IllegalArgumentException("decision status must be terminal");
        }
        if (decidedAt == null) {
            throw new IllegalArgumentException("decidedAt must not be null");
        }
        requestId = requestId.trim();
        actorId = normalize(actorId);
        channel = normalize(channel);
        comment = normalize(comment);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
