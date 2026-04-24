package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import java.time.Instant;
import java.util.List;

public record LotRun(
        String lotId,
        String lotCode,
        int executionOrder,
        LotRunStatus status,
        int correctionAttemptCount,
        List<String> artifactReferences,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant completedAt,
        String summary
) {

    public LotRun {
        if (isBlank(lotId)) {
            throw new IllegalArgumentException("lotId must not be blank");
        }
        if (isBlank(lotCode)) {
            throw new IllegalArgumentException("lotCode must not be blank");
        }
        if (executionOrder < 0) {
            throw new IllegalArgumentException("executionOrder must be greater than or equal to 0");
        }
        if (correctionAttemptCount < 0) {
            throw new IllegalArgumentException("correctionAttemptCount must be greater than or equal to 0");
        }

        status = status == null ? LotRunStatus.PENDING : status;
        artifactReferences = artifactReferences == null ? List.of() : List.copyOf(artifactReferences);

        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (startedAt != null && startedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("startedAt must not be before createdAt");
        }
        if (completedAt != null && startedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completedAt must not be before startedAt");
        }

        lotId = lotId.trim();
        lotCode = lotCode.trim();
        summary = normalize(summary);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
