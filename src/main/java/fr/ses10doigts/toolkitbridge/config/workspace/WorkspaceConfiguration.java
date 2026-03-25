package fr.ses10doigts.toolkitbridge.config.workspace;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfiguration {
}