package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

@Component
public class AdminTaskSnapshotMapper {

    public AdminTaskSnapshotEntity toEntityForInsert(Task task,
                                                     String channelType,
                                                     String conversationId,
                                                     String errorMessage,
                                                     Instant now) {
        AdminTaskSnapshotEntity entity = new AdminTaskSnapshotEntity();
        entity.setTaskId(task.taskId());
        entity.setParentTaskId(task.parentTaskId());
        entity.setObjective(task.objective());
        entity.setInitiator(task.initiator());
        entity.setAssignedAgentId(normalizeAgentId(task.assignedAgentId()));
        entity.setTraceId(task.traceId());
        entity.setEntryPoint(task.entryPoint());
        entity.setStatus(task.status());
        entity.setChannelType(channelType);
        entity.setConversationId(conversationId);
        entity.setFirstSeenAt(now);
        entity.setLastSeenAt(now);
        entity.setErrorMessage(normalizeMessage(errorMessage));
        entity.setArtifactCount(task.artifacts() == null ? 0 : task.artifacts().size());
        return entity;
    }

    public AdminTaskSnapshot toDomain(AdminTaskSnapshotEntity entity) {
        return new AdminTaskSnapshot(
                entity.getTaskId(),
                entity.getParentTaskId(),
                entity.getObjective(),
                entity.getInitiator(),
                entity.getAssignedAgentId(),
                entity.getTraceId(),
                entity.getEntryPoint(),
                entity.getStatus(),
                entity.getChannelType(),
                entity.getConversationId(),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt(),
                entity.getErrorMessage(),
                entity.getArtifactCount()
        );
    }

    public String normalizeAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        // Stored as a technical identifier for filtering/indexing (not for display rendering).
        return agentId.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeMessage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
