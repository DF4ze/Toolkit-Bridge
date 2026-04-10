package fr.ses10doigts.toolkitbridge.persistence.retention;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "toolkit.persistence.retention.cleanup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PersistenceRetentionCleanupScheduler {

    private final PersistenceRetentionCleanupService cleanupService;

    public PersistenceRetentionCleanupScheduler(PersistenceRetentionCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "${toolkit.persistence.retention.cleanup.cron:" + PersistenceRetentionCleanupProperties.DEFAULT_CRON + "}")
    public void cleanupPersistenceData() {
        cleanupService.cleanup();
    }
}
