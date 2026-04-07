package fr.ses10doigts.toolkitbridge.service.agent.trace.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentTraceProperties.class)
public class AgentTraceConfiguration {
}
