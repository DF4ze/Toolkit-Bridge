package fr.ses10doigts.toolkitbridge.persistence.retention;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.persistence.retention.cleanup")
@Data
public class PersistenceRetentionCleanupProperties {

    private boolean enabled = true;
    private String cron = "0 0 3 * * *";
}

