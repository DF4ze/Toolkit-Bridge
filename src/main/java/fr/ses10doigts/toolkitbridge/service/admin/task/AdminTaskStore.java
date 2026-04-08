package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;

import java.util.List;

public interface AdminTaskStore {

    void record(Task task, String channelType, String conversationId, String errorMessage);

    List<AdminTaskSnapshot> recent(int limit, String agentId, TaskStatus status);
}
