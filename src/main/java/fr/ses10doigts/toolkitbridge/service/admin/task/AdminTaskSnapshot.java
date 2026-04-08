package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;

import java.time.Instant;

public record AdminTaskSnapshot(
        String taskId,
        String parentTaskId,
        String objective,
        String initiator,
        String assignedAgentId,
        String traceId,
        TaskEntryPoint entryPoint,
        TaskStatus status,
        String channelType,
        String conversationId,
        Instant firstSeenAt,
        Instant lastSeenAt,
        String errorMessage,
        int artifactCount
) {
}
