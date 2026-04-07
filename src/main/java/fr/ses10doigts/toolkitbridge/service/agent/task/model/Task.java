package fr.ses10doigts.toolkitbridge.service.agent.task.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Task(
        String taskId,
        String objective,
        String initiator,
        String assignedAgentId,
        String parentTaskId,
        String traceId,
        TaskEntryPoint entryPoint,
        TaskStatus status,
        Map<String, Object> metadata,
        List<ArtifactReference> artifacts
) implements DurableObject {
    public Task {
        if (isBlank(taskId)) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (isBlank(objective)) {
            throw new IllegalArgumentException("objective must not be blank");
        }
        if (isBlank(initiator)) {
            throw new IllegalArgumentException("initiator must not be blank");
        }
        if (isBlank(assignedAgentId)) {
            throw new IllegalArgumentException("assignedAgentId must not be blank");
        }
        if (isBlank(traceId)) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        Objects.requireNonNull(entryPoint, "entryPoint must not be null");
        status = status == null ? TaskStatus.CREATED : status;

        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);

        if (entryPoint == TaskEntryPoint.DELEGATION_SUBTASK && isBlank(parentTaskId)) {
            throw new IllegalArgumentException("parentTaskId must not be blank for delegated subtasks");
        }
        if (entryPoint == TaskEntryPoint.TASK_ORCHESTRATOR && !isBlank(parentTaskId)) {
            throw new IllegalArgumentException("parentTaskId must be blank for root task entry point");
        }
    }

    public boolean isSubTask() {
        return !isBlank(parentTaskId);
    }

    public Task withArtifact(ArtifactReference artifact) {
        Objects.requireNonNull(artifact, "artifact must not be null");
        List<ArtifactReference> updatedArtifacts = new java.util.ArrayList<>(artifacts);
        updatedArtifacts.add(artifact);
        return new Task(
                taskId,
                objective,
                initiator,
                assignedAgentId,
                parentTaskId,
                traceId,
                entryPoint,
                status,
                metadata,
                updatedArtifacts
        );
    }

    public Task transitionTo(TaskStatus targetStatus) {
        TaskLifecycle.requireValidTransition(taskId, status, targetStatus);
        if (status == targetStatus) {
            return this;
        }
        return new Task(
                taskId,
                objective,
                initiator,
                assignedAgentId,
                parentTaskId,
                traceId,
                entryPoint,
                targetStatus,
                metadata,
                artifacts
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.TASK;
    }
}
