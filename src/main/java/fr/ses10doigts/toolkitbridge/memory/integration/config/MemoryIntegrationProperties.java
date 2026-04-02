package fr.ses10doigts.toolkitbridge.memory.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "toolkit.memory.integration")
public class MemoryIntegrationProperties {

    private boolean enableSemanticExtraction = true;
    private boolean enableRulePromotion = true;
    private boolean enableEpisodicInjection = true;
    private boolean markUsedEnabled = true;
    private int maxRules = 10;
    private int maxSemanticMemories = 10;
    private int maxEpisodes = 5;
    private int maxConversationMessages = 20;
    private int maxContextCharacters = 15000;
}
