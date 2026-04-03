package fr.ses10doigts.toolkitbridge.service.agent.artifact.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ArtifactStorageProperties.class)
@ConditionalOnProperty(prefix = "toolkit.artifacts", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ArtifactStorageConfiguration {
}
