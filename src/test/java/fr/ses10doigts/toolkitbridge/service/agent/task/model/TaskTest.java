package fr.ses10doigts.toolkitbridge.service.agent.task.model;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
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
        List<ArtifactReference> artifacts = List.of(new ArtifactReference("artifact-1", ArtifactType.FILE));

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

        Task updated = task.withArtifact(new ArtifactReference("artifact-2", ArtifactType.REPORT));

        assertThat(task.artifacts()).isEmpty();
        assertThat(updated.artifacts()).hasSize(1);
        assertThat(updated.artifacts().getFirst().artifactId()).isEqualTo("artifact-2");
        assertThat(updated.artifacts().getFirst().type()).isEqualTo(ArtifactType.REPORT);
    }
}
