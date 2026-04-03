package fr.ses10doigts.toolkitbridge.service.agent.task.service;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskFactoryTest {

    private final TaskFactory taskFactory = new TaskFactory();

    @Test
    void createsObjectiveTask() {
        Task task = taskFactory.createObjectiveTask(
                "Build release checklist",
                "user-1",
                "agent-1",
                "trace-1",
                Map.of("priority", "high")
        );

        assertThat(task.entryPoint()).isEqualTo(TaskEntryPoint.TASK_ORCHESTRATOR);
        assertThat(task.status()).isEqualTo(TaskStatus.CREATED);
        assertThat(task.parentTaskId()).isNull();
        assertThat(task.objective()).isEqualTo("Build release checklist");
        assertThat(task.metadata()).containsEntry("priority", "high");
    }

    @Test
    void defaultsMetadataWhenMissing() {
        Task task = taskFactory.createObjectiveTask(
                "Build release checklist",
                "user-1",
                "agent-1",
                "trace-1",
                null
        );

        assertThat(task.metadata()).isEmpty();
    }

    @Test
    void createsDelegatedSubTaskWithParentReference() {
        Task parent = new Task(
                "parent-1",
                "Parent objective",
                "user-1",
                "agent-1",
                null,
                "trace-parent",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                Map.of(),
                java.util.List.of()
        );

        Task subTask = taskFactory.createDelegatedSubTask(
                parent,
                "Child objective",
                "agent-2",
                "agent-1",
                "trace-child",
                Map.of("delegatedBy", "agent-1")
        );

        assertThat(subTask.entryPoint()).isEqualTo(TaskEntryPoint.DELEGATION_SUBTASK);
        assertThat(subTask.parentTaskId()).isEqualTo("parent-1");
        assertThat(subTask.status()).isEqualTo(TaskStatus.CREATED);
    }
}
