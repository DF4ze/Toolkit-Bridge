package fr.ses10doigts.toolkitbridge.service.agent.task.service;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class TaskFactory {

    public Task createObjectiveTask(String objective,
                                    String initiator,
                                    String assignedAgentId,
                                    String traceId,
                                    Map<String, Object> metadata) {
        return new Task(
                UUID.randomUUID().toString(),
                objective,
                initiator,
                assignedAgentId,
                null,
                traceId,
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                metadata == null ? Map.of() : metadata,
                java.util.List.of()
        );
    }

    public Task createDelegatedSubTask(Task parentTask,
                                       String objective,
                                       String assignedAgentId,
                                       String initiator,
                                       String traceId,
                                       Map<String, Object> metadata) {
        if (parentTask == null) {
            throw new IllegalArgumentException("parentTask must not be null");
        }
        return new Task(
                UUID.randomUUID().toString(),
                objective,
                initiator,
                assignedAgentId,
                parentTask.taskId(),
                traceId,
                TaskEntryPoint.DELEGATION_SUBTASK,
                TaskStatus.CREATED,
                metadata,
                java.util.List.of()
        );
    }
}
