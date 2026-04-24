package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowRun(
        String runId,
        String workflowType,
        String targetStepRef,
        WorkflowRunStatus status,
        List<LotRun> lotRuns,
        String currentLotId,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant completedAt,
        Map<String, Object> context
) {

    public WorkflowRun {
        if (isBlank(runId)) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (isBlank(workflowType)) {
            throw new IllegalArgumentException("workflowType must not be blank");
        }
        if (isBlank(targetStepRef)) {
            throw new IllegalArgumentException("targetStepRef must not be blank");
        }

        status = status == null ? WorkflowRunStatus.NEW : status;
        lotRuns = lotRuns == null ? List.of() : List.copyOf(lotRuns);
        context = context == null ? Map.of() : Map.copyOf(context);

        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;

        runId = runId.trim();
        workflowType = workflowType.trim();
        targetStepRef = targetStepRef.trim();
        currentLotId = normalize(currentLotId);

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
        if (startedAt != null && startedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("startedAt must not be before createdAt");
        }
        if (completedAt != null && startedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completedAt must not be before startedAt");
        }
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
