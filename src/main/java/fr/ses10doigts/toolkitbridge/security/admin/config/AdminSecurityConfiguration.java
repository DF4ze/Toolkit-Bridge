package fr.ses10doigts.toolkitbridge.security.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfiguration {
}
