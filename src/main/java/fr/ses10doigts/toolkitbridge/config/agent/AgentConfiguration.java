package fr.ses10doigts.toolkitbridge.config.agent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(AgentsProperties.class)
@Configuration
public class AgentConfiguration {
}