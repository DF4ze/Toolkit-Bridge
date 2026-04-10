package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshotRepository;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PersistenceRetentionCleanupServiceTest {

    @Test
    void cleanupPurgesCriticalTracesAndAdminTaskSnapshotsWhenPoliciesAllowIt() {
        PersistenceRetentionPolicyResolver resolver = mock(PersistenceRetentionPolicyResolver.class);
        CriticalAgentTraceRepository traceRepository = mock(CriticalAgentTraceRepository.class);
        AdminTaskSnapshotRepository taskRepository = mock(AdminTaskSnapshotRepository.class);

        when(resolver.resolve(PersistableObjectFamily.TRACE, RetentionDomains.TRACE_CRITICAL))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TRACE,
                        RetentionDomains.TRACE_CRITICAL,
                        Duration.ofDays(7),
                        RetentionDisposition.PURGE
                ));
        when(resolver.resolve(PersistableObjectFamily.TASK, RetentionDomains.TASK_ADMIN_SNAPSHOT))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TASK,
                        RetentionDomains.TASK_ADMIN_SNAPSHOT,
                        Duration.ofDays(30),
                        RetentionDisposition.PURGE
                ));
        when(traceRepository.deleteByOccurredAtBefore(any(Instant.class))).thenReturn(3L);
        when(taskRepository.deleteByLastSeenAtBefore(any(Instant.class))).thenReturn(5L);

        PersistenceRetentionCleanupService service = new PersistenceRetentionCleanupService(
                resolver,
                traceRepository,
                taskRepository
        );

        service.cleanup();

        verify(traceRepository).deleteByOccurredAtBefore(any(Instant.class));
        verify(taskRepository).deleteByLastSeenAtBefore(any(Instant.class));
    }

    @Test
    void cleanupSkipsWhenDispositionIsNotPurge() {
        PersistenceRetentionPolicyResolver resolver = mock(PersistenceRetentionPolicyResolver.class);
        CriticalAgentTraceRepository traceRepository = mock(CriticalAgentTraceRepository.class);
        AdminTaskSnapshotRepository taskRepository = mock(AdminTaskSnapshotRepository.class);

        when(resolver.resolve(PersistableObjectFamily.TRACE, RetentionDomains.TRACE_CRITICAL))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TRACE,
                        RetentionDomains.TRACE_CRITICAL,
                        Duration.ofDays(7),
                        RetentionDisposition.PRESERVE
                ));
        when(resolver.resolve(PersistableObjectFamily.TASK, RetentionDomains.TASK_ADMIN_SNAPSHOT))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TASK,
                        RetentionDomains.TASK_ADMIN_SNAPSHOT,
                        Duration.ofDays(30),
                        RetentionDisposition.ARCHIVE
                ));

        PersistenceRetentionCleanupService service = new PersistenceRetentionCleanupService(
                resolver,
                traceRepository,
                taskRepository
        );

        service.cleanup();

        verify(traceRepository, never()).deleteByOccurredAtBefore(any(Instant.class));
        verify(taskRepository, never()).deleteByLastSeenAtBefore(any(Instant.class));
    }

    @Test
    void cleanupSkipsWhenTtlIsNullOrNonPositive() {
        PersistenceRetentionPolicyResolver resolver = mock(PersistenceRetentionPolicyResolver.class);
        CriticalAgentTraceRepository traceRepository = mock(CriticalAgentTraceRepository.class);
        AdminTaskSnapshotRepository taskRepository = mock(AdminTaskSnapshotRepository.class);

        when(resolver.resolve(PersistableObjectFamily.TRACE, RetentionDomains.TRACE_CRITICAL))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TRACE,
                        RetentionDomains.TRACE_CRITICAL,
                        Duration.ZERO,
                        RetentionDisposition.PURGE
                ));
        when(resolver.resolve(PersistableObjectFamily.TASK, RetentionDomains.TASK_ADMIN_SNAPSHOT))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TASK,
                        RetentionDomains.TASK_ADMIN_SNAPSHOT,
                        null,
                        RetentionDisposition.PURGE
                ));

        PersistenceRetentionCleanupService service = new PersistenceRetentionCleanupService(
                resolver,
                traceRepository,
                taskRepository
        );

        service.cleanup();

        verify(traceRepository, never()).deleteByOccurredAtBefore(any(Instant.class));
        verify(taskRepository, never()).deleteByLastSeenAtBefore(any(Instant.class));
    }

    @Test
    void cleanupContinuesOnTaskPurgeWhenTracePurgeFails() {
        PersistenceRetentionPolicyResolver resolver = mock(PersistenceRetentionPolicyResolver.class);
        CriticalAgentTraceRepository traceRepository = mock(CriticalAgentTraceRepository.class);
        AdminTaskSnapshotRepository taskRepository = mock(AdminTaskSnapshotRepository.class);

        when(resolver.resolve(eq(PersistableObjectFamily.TRACE), eq(RetentionDomains.TRACE_CRITICAL)))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TRACE,
                        RetentionDomains.TRACE_CRITICAL,
                        Duration.ofDays(7),
                        RetentionDisposition.PURGE
                ));
        when(resolver.resolve(eq(PersistableObjectFamily.TASK), eq(RetentionDomains.TASK_ADMIN_SNAPSHOT)))
                .thenReturn(new RetentionPolicy(
                        PersistableObjectFamily.TASK,
                        RetentionDomains.TASK_ADMIN_SNAPSHOT,
                        Duration.ofDays(30),
                        RetentionDisposition.PURGE
                ));
        when(traceRepository.deleteByOccurredAtBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("db down"));
        when(taskRepository.deleteByLastSeenAtBefore(any(Instant.class))).thenReturn(2L);

        PersistenceRetentionCleanupService service = new PersistenceRetentionCleanupService(
                resolver,
                traceRepository,
                taskRepository
        );

        service.cleanup();

        verify(traceRepository).deleteByOccurredAtBefore(any(Instant.class));
        verify(taskRepository).deleteByLastSeenAtBefore(any(Instant.class));
    }
}
