package fr.ses10doigts.toolkitbridge.persistence.retention;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.persistence.retention.cleanup")
@Data
public class PersistenceRetentionCleanupProperties {

    public static final String DEFAULT_CRON = "0 0 3 * * *";

    private boolean enabled = true;
    private String cron = DEFAULT_CRON;
}
