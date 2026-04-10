package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshotRepository;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class PersistenceRetentionCleanupService {

    private final PersistenceRetentionPolicyResolver retentionPolicyResolver;
    private final CriticalAgentTraceRepository criticalAgentTraceRepository;
    private final AdminTaskSnapshotRepository adminTaskSnapshotRepository;

    public PersistenceRetentionCleanupService(PersistenceRetentionPolicyResolver retentionPolicyResolver,
                                              CriticalAgentTraceRepository criticalAgentTraceRepository,
                                              AdminTaskSnapshotRepository adminTaskSnapshotRepository) {
        this.retentionPolicyResolver = retentionPolicyResolver;
        this.criticalAgentTraceRepository = criticalAgentTraceRepository;
        this.adminTaskSnapshotRepository = adminTaskSnapshotRepository;
    }

    @Transactional
    public void cleanup() {
        cleanupCriticalTraces();
        cleanupAdminTaskSnapshots();
    }

    void cleanupCriticalTraces() {
        runCleanup(
                "critical_trace",
                PersistableObjectFamily.TRACE,
                RetentionDomains.TRACE_CRITICAL,
                criticalAgentTraceRepository::deleteByOccurredAtBefore
        );
    }

    void cleanupAdminTaskSnapshots() {
        runCleanup(
                "admin_task_snapshot",
                PersistableObjectFamily.TASK,
                RetentionDomains.TASK_ADMIN_SNAPSHOT,
                adminTaskSnapshotRepository::deleteByLastSeenAtBefore
        );
    }

    private void runCleanup(String target,
                            PersistableObjectFamily family,
                            String domain,
                            CutoffDeletion deletion) {
        RetentionPolicy policy = retentionPolicyResolver.resolve(family, domain);
        Duration ttl = policy.ttl();

        if (policy.disposition() != RetentionDisposition.PURGE) {
            log.warn("Retention cleanup skipped target={} family={} domain={} disposition={}",
                    target, family, domain, policy.disposition());
            return;
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            log.warn("Retention cleanup skipped target={} family={} domain={} ttl={}",
                    target, family, domain, ttl);
            return;
        }

        Instant cutoff = Instant.now().minus(ttl);
        long startNanos = System.nanoTime();
        try {
            long deletedRows = deletion.delete(cutoff);
            log.info("Retention cleanup done target={} family={} domain={} ttl={} cutoff={} deletedRows={} durationMs={}",
                    target, family, domain, ttl, cutoff, deletedRows, elapsedMs(startNanos));
        } catch (RuntimeException e) {
            log.error("Retention cleanup failed target={} family={} domain={} cutoff={}",
                    target, family, domain, cutoff, e);
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @FunctionalInterface
    private interface CutoffDeletion {
        long delete(Instant cutoff);
    }
}
