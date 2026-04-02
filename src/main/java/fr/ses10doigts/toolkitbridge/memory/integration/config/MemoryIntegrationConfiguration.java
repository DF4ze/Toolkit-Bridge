package fr.ses10doigts.toolkitbridge.memory.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MemoryIntegrationProperties.class)
public class MemoryIntegrationConfiguration {
}
