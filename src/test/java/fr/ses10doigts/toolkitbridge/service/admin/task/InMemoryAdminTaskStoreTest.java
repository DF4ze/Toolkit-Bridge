package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAdminTaskStoreTest {

    @Test
    void keepsLatestSnapshotAndFiltersByAgentAndStatus() {
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.getTasks().setMaxEvents(10);
        InMemoryAdminTaskStore store = new InMemoryAdminTaskStore(properties);

        Task created = task("task-1", "agent-a", TaskStatus.CREATED);
        Task running = created.transitionTo(TaskStatus.RUNNING);
        Task done = running.transitionTo(TaskStatus.DONE);

        store.record(created, "telegram", "conv-1", null);
        store.record(running, "telegram", "conv-1", null);
        store.record(done, "telegram", "conv-1", null);
        store.record(task("task-2", "agent-b", TaskStatus.FAILED), "telegram", "conv-2", "boom");

        assertThat(store.recent(10, null, null)).hasSize(2);
        assertThat(store.recent(10, "agent-a", TaskStatus.DONE))
                .hasSize(1)
                .first()
                .extracting(AdminTaskSnapshot::taskId)
                .isEqualTo("task-1");
    }

    @Test
    void evictsOldestWhenCapacityIsReached() {
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.getTasks().setMaxEvents(2);
        InMemoryAdminTaskStore store = new InMemoryAdminTaskStore(properties);

        store.record(task("task-1", "agent-a", TaskStatus.CREATED), "telegram", "conv-1", null);
        store.record(task("task-2", "agent-a", TaskStatus.CREATED), "telegram", "conv-1", null);
        store.record(task("task-3", "agent-a", TaskStatus.CREATED), "telegram", "conv-1", null);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.recent(10, null, null))
                .extracting(AdminTaskSnapshot::taskId)
                .containsExactly("task-3", "task-2");
    }

    private Task task(String taskId, String agentId, TaskStatus status) {
        return new Task(
                taskId,
                "objective",
                "user-1",
                agentId,
                null,
                "trace-1",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                status,
                Map.of(),
                java.util.List.of()
        );
    }
}
