package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshot;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminTaskQueryService {

    private final AdminTaskStore taskStore;
    private final AdminTechnicalProperties technicalProperties;

    public AdminTaskQueryService(AdminTaskStore taskStore, AdminTechnicalProperties technicalProperties) {
        this.taskStore = taskStore;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.TaskItem> listRecentTasks(Integer limit, String agentId, TaskStatus status) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        return taskStore.recent(effectiveLimit, agentId, status).stream()
                .map(this::toTaskItem)
                .toList();
    }

    private TechnicalAdminView.TaskItem toTaskItem(AdminTaskSnapshot task) {
        return new TechnicalAdminView.TaskItem(
                task.taskId(),
                task.parentTaskId(),
                task.objective(),
                task.initiator(),
                task.assignedAgentId(),
                task.traceId(),
                task.entryPoint() == null ? null : task.entryPoint().name(),
                task.status(),
                task.channelType(),
                task.conversationId(),
                task.firstSeenAt(),
                task.lastSeenAt(),
                task.errorMessage(),
                task.artifactCount()
        );
    }
}

