package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TelegramSupervisionProperties.class)
public class TelegramSupervisionConfiguration {
}
