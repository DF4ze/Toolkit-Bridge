package fr.ses10doigts.toolkitbridge.memory.context.config;

import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ContextAssemblerProperties.class, GlobalContextProperties.class})
public class ContextAssemblerConfiguration {
}
