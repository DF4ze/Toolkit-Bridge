package fr.ses10doigts.toolkitbridge.service.agent.task.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    @Test
    void createsObjectiveTaskWithImmutableCollections() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority", "high");
        List<TaskArtifactRef> artifacts = List.of(new TaskArtifactRef("artifact-1", "text", "/tmp/a.txt"));

        Task task = new Task(
                "task-1",
                "Prepare release plan",
                "user-1",
                "agent-1",
                null,
                "trace-1",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                metadata,
                artifacts
        );

        metadata.put("priority", "low");

        assertThat(task.taskId()).isEqualTo("task-1");
        assertThat(task.objective()).isEqualTo("Prepare release plan");
        assertThat(task.isSubTask()).isFalse();
        assertThat(task.metadata()).containsEntry("priority", "high");
        assertThatThrownBy(() -> task.metadata().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresParentTaskForDelegatedSubTask() {
        assertThatThrownBy(() -> new Task(
                "task-2",
                "Write tests",
                "agent-1",
                "agent-2",
                null,
                "trace-2",
                TaskEntryPoint.DELEGATION_SUBTASK,
                TaskStatus.CREATED,
                Map.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parentTaskId");
    }

    @Test
    void rejectsParentTaskOnRootEntryPoint() {
        assertThatThrownBy(() -> new Task(
                "task-2b",
                "Write tests",
                "agent-1",
                "agent-2",
                "parent-1",
                "trace-2",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                Map.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("root task entry point");
    }

    @Test
    void addsArtifactThroughImmutableCopy() {
        Task task = new Task(
                "task-3",
                "Collect logs",
                "user-2",
                "agent-3",
                null,
                "trace-3",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                Map.of(),
                List.of()
        );

        Task updated = task.withArtifact(new TaskArtifactRef("artifact-2", "log", "/tmp/log.txt"));

        assertThat(task.artifacts()).isEmpty();
        assertThat(updated.artifacts()).hasSize(1);
        assertThat(updated.artifacts().getFirst().artifactId()).isEqualTo("artifact-2");
    }
}
