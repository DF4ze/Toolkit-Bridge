package fr.ses10doigts.toolkitbridge.persistence.retention;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PersistenceRetentionProperties.class)
public class PersistenceRetentionConfiguration {
}
