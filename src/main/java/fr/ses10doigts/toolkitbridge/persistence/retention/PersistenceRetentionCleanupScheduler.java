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
    private final PersistenceRetentionCleanupProperties cleanupProperties;

    public PersistenceRetentionCleanupScheduler(PersistenceRetentionCleanupService cleanupService,
                                                PersistenceRetentionCleanupProperties cleanupProperties) {
        this.cleanupService = cleanupService;
        this.cleanupProperties = cleanupProperties;
    }

    @Scheduled(cron = "#{@persistenceRetentionCleanupScheduler.cronExpression()}")
    public void cleanupPersistenceData() {
        cleanupService.cleanup();
    }

    public String cronExpression() {
        return cleanupProperties.getCron();
    }
}
