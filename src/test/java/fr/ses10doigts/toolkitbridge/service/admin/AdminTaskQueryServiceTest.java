package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshot;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTaskQueryServiceTest {

    @Test
    void listRecentTasksSanitizesLimitAndMapsSnapshots() {
        AdminTaskStore taskStore = mock(AdminTaskStore.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setDefaultListLimit(5);
        properties.setMaxListLimit(2);

        when(taskStore.recent(2, "agent-1", TaskStatus.RUNNING)).thenReturn(List.of(
                snapshot("task-1", TaskStatus.RUNNING, TaskEntryPoint.TASK_ORCHESTRATOR),
                snapshot("task-2", TaskStatus.RUNNING, null)
        ));

        AdminTaskQueryService service = new AdminTaskQueryService(taskStore, properties);

        List<TechnicalAdminView.TaskItem> items = service.listRecentTasks(999, "agent-1", TaskStatus.RUNNING);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).taskId()).isEqualTo("task-1");
        assertThat(items.get(0).entryPoint()).isEqualTo("TASK_ORCHESTRATOR");
        assertThat(items.get(0).status()).isEqualTo(TaskStatus.RUNNING);
        assertThat(items.get(1).taskId()).isEqualTo("task-2");
        assertThat(items.get(1).entryPoint()).isNull();
        verify(taskStore).recent(2, "agent-1", TaskStatus.RUNNING);
    }

    private AdminTaskSnapshot snapshot(String taskId, TaskStatus status, TaskEntryPoint entryPoint) {
        return new AdminTaskSnapshot(
                taskId,
                null,
                "objective",
                "user-1",
                "agent-1",
                "trace-" + taskId,
                entryPoint,
                status,
                "telegram",
                "conv-1",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:10Z"),
                null,
                2
        );
    }
}

