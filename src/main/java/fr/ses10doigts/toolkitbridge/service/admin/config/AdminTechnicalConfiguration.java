package fr.ses10doigts.toolkitbridge.service.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminTechnicalProperties.class)
public class AdminTechnicalConfiguration {
}
