package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PersistentAdminTaskStoreTest {

    @Test
    void recordCreatesSnapshotWhenUpdateDoesNotMatch() {
        AdminTaskSnapshotRepository repository = mock(AdminTaskSnapshotRepository.class);
        when(repository.updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(0);
        when(repository.save(any(AdminTaskSnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersistentAdminTaskStore store = new PersistentAdminTaskStore(repository, new AdminTaskSnapshotMapper());
        Task task = task("task-1", " Agent-A ", TaskStatus.CREATED, List.of(
                new ArtifactReference("artifact-1", ArtifactType.REPORT),
                new ArtifactReference("artifact-2", ArtifactType.FILE)
        ));

        store.record(task, "telegram", "conv-1", "  boom  ");

        ArgumentCaptor<AdminTaskSnapshotEntity> captor = ArgumentCaptor.forClass(AdminTaskSnapshotEntity.class);
        verify(repository).save(captor.capture());
        AdminTaskSnapshotEntity saved = captor.getValue();

        assertThat(saved.getTaskId()).isEqualTo("task-1");
        assertThat(saved.getAssignedAgentId()).isEqualTo("agent-a");
        assertThat(saved.getErrorMessage()).isEqualTo("boom");
        assertThat(saved.getArtifactCount()).isEqualTo(2);
        assertThat(saved.getFirstSeenAt()).isNotNull();
        assertThat(saved.getLastSeenAt()).isNotNull();
    }

    @Test
    void recordUpdatesExistingSnapshotWithoutInsert() {
        AdminTaskSnapshotRepository repository = mock(AdminTaskSnapshotRepository.class);
        when(repository.updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(1);

        PersistentAdminTaskStore store = new PersistentAdminTaskStore(repository, new AdminTaskSnapshotMapper());

        store.record(task("task-1", "agent-a", TaskStatus.RUNNING, List.of()), "telegram", "conv-1", null);

        verify(repository, never()).save(any(AdminTaskSnapshotEntity.class));
    }

    @Test
    void recentNormalizesAgentFilterBeforeQuery() {
        AdminTaskSnapshotRepository repository = mock(AdminTaskSnapshotRepository.class);
        PersistentAdminTaskStore store = new PersistentAdminTaskStore(repository, new AdminTaskSnapshotMapper());

        store.recent(10, " Agent-A ", TaskStatus.DONE);

        verify(repository).findByAssignedAgentIdAndStatusOrderByLastSeenAtDescTaskIdDesc(eq("agent-a"), eq(TaskStatus.DONE), any());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void recordLogsWarningWhenInsertConflictFallbackUpdateDoesNotModifyRow(CapturedOutput output) {
        AdminTaskSnapshotRepository repository = mock(AdminTaskSnapshotRepository.class);
        when(repository.updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(0, 0);
        when(repository.save(any(AdminTaskSnapshotEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PersistentAdminTaskStore store = new PersistentAdminTaskStore(repository, new AdminTaskSnapshotMapper());
        store.record(task("task-warn", "agent-a", TaskStatus.CREATED, List.of()), "telegram", "conv-1", null);

        verify(repository, times(2)).updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        );
        assertThat(output.getOut()).contains("taskId=task-warn");
    }

    @Test
    void recordRetriesUpdateAfterInsertConflict() {
        AdminTaskSnapshotRepository repository = mock(AdminTaskSnapshotRepository.class);
        when(repository.updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(0, 1);
        when(repository.save(any(AdminTaskSnapshotEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        PersistentAdminTaskStore store = new PersistentAdminTaskStore(repository, new AdminTaskSnapshotMapper());
        store.record(task("task-retry", "agent-a", TaskStatus.CREATED, List.of()), "telegram", "conv-1", null);

        verify(repository, times(2)).updateSnapshotByTaskId(
                anyString(), any(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), any(), any(), anyInt()
        );
    }

    private Task task(String taskId, String assignedAgentId, TaskStatus status, List<ArtifactReference> artifacts) {
        return new Task(
                taskId,
                "objective",
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
