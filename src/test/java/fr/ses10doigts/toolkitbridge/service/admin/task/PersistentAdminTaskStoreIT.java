package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/admin-task-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.llm.openai-like.providers[0].name=seed",
                "toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                "toolkit.llm.openai-like.providers[0].api-key=",
                "toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistentAdminTaskStoreIT {

    @Autowired
    private AdminTaskStore store;

    @Autowired
    private AdminTaskSnapshotRepository repository;

    @Test
    void recordShouldCreateThenUpdateWithoutDuplicatingAndKeepFirstSeenAt() throws Exception {
        String taskId = "task-" + UUID.randomUUID();
        Task created = task(taskId, " Agent-A ", TaskStatus.CREATED, List.of());
        store.record(created, "telegram", "conv-1", "  boom  ");

        Optional<AdminTaskSnapshotEntity> firstPersisted = repository.findByTaskId(taskId);
        assertThat(firstPersisted).isPresent();
        Instant firstSeenAt = firstPersisted.orElseThrow().getFirstSeenAt();
        Instant firstLastSeenAt = firstPersisted.orElseThrow().getLastSeenAt();

        Thread.sleep(5);
        Task done = created.transitionTo(TaskStatus.RUNNING)
                .transitionTo(TaskStatus.DONE)
                .withArtifact(new ArtifactReference("artifact-1", ArtifactType.REPORT));
        store.record(done, "telegram", "conv-1", null);

        Optional<AdminTaskSnapshotEntity> secondPersisted = repository.findByTaskId(taskId);
        assertThat(secondPersisted).isPresent();
        AdminTaskSnapshotEntity entity = secondPersisted.orElseThrow();

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(entity.getAssignedAgentId()).isEqualTo("agent-a");
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getArtifactCount()).isEqualTo(1);
        assertThat(entity.getFirstSeenAt()).isEqualTo(firstSeenAt);
        assertThat(entity.getLastSeenAt()).isAfterOrEqualTo(firstLastSeenAt);
    }

    @Test
    void recentShouldRespectSortFiltersAndLimit() throws Exception {
        Task t1 = task("task-" + UUID.randomUUID(), "agent-a", TaskStatus.CREATED, List.of());
        Task t2 = task("task-" + UUID.randomUUID(), "agent-b", TaskStatus.RUNNING, List.of());
        Task t3 = task("task-" + UUID.randomUUID(), "agent-a", TaskStatus.FAILED, List.of());

        store.record(t1, "telegram", "conv-1", null);
        Thread.sleep(5);
        store.record(t2, "telegram", "conv-2", null);
        Thread.sleep(5);
        store.record(t3, "telegram", "conv-3", "boom");

        List<AdminTaskSnapshot> recent = store.recent(2, null, null);
        List<AdminTaskSnapshot> byAgent = store.recent(10, " Agent-A ", null);
        List<AdminTaskSnapshot> byStatus = store.recent(10, null, TaskStatus.FAILED);

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).taskId()).isEqualTo(t3.taskId());
        assertThat(recent.get(1).taskId()).isEqualTo(t2.taskId());
        assertThat(byAgent).extracting(AdminTaskSnapshot::taskId).containsExactly(t3.taskId(), t1.taskId());
        assertThat(byAgent).extracting(AdminTaskSnapshot::assignedAgentId).containsOnly("agent-a");
        assertThat(byStatus).extracting(AdminTaskSnapshot::taskId).containsExactly(t3.taskId());
    }

    @Test
    void recentShouldUseDeterministicOrderWhenLastSeenAtIsEqual() {
        String taskA = "task-det-a";
        String taskB = "task-det-b";
        Task first = task(taskA, "agent-det", TaskStatus.RUNNING, List.of());
        Task second = task(taskB, "agent-det", TaskStatus.RUNNING, List.of());

        store.record(first, "telegram", "conv-a", null);
        store.record(second, "telegram", "conv-b", null);

        Instant sameTimestamp = Instant.parse("2026-01-01T10:00:00Z");
        forceLastSeenAt(taskA, sameTimestamp);
        forceLastSeenAt(taskB, sameTimestamp);

        List<AdminTaskSnapshot> ordered = store.recent(10, "agent-det", TaskStatus.RUNNING);
        assertThat(ordered).extracting(AdminTaskSnapshot::taskId).containsSequence(taskB, taskA);
    }

    private void forceLastSeenAt(String taskId, Instant lastSeenAt) {
        AdminTaskSnapshotEntity entity = repository.findByTaskId(taskId).orElseThrow();
        entity.setLastSeenAt(lastSeenAt);
        repository.save(entity);
    }

    private Task task(String taskId, String assignedAgentId, TaskStatus status, List<ArtifactReference> artifacts) {
        return new Task(
                taskId,
                "objective-" + taskId,
                "user-1",
                assignedAgentId,
                null,
                "trace-" + taskId,
                TaskEntryPoint.TASK_ORCHESTRATOR,
                status,
                Map.of(),
                artifacts
        );
    }
}
