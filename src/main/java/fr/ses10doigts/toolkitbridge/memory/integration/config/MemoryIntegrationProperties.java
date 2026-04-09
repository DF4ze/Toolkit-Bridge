package fr.ses10doigts.toolkitbridge.memory.integration.config;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryConfigurationDefaults;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "toolkit.memory.integration")
public class MemoryIntegrationProperties {

    private boolean enableSemanticExtraction = MemoryConfigurationDefaults.INTEGRATION_ENABLE_SEMANTIC_EXTRACTION;
    private boolean enableRulePromotion = MemoryConfigurationDefaults.INTEGRATION_ENABLE_RULE_PROMOTION;
    private boolean enableEpisodicInjection = MemoryConfigurationDefaults.INTEGRATION_ENABLE_EPISODIC_INJECTION;
    private boolean markUsedEnabled = MemoryConfigurationDefaults.INTEGRATION_MARK_USED_ENABLED;
    private int maxRules = MemoryConfigurationDefaults.INTEGRATION_MAX_RULES;
    private int maxSemanticMemories = MemoryConfigurationDefaults.INTEGRATION_MAX_SEMANTIC_MEMORIES;
    private int maxEpisodes = MemoryConfigurationDefaults.INTEGRATION_MAX_EPISODES;
    private int maxConversationMessages = MemoryConfigurationDefaults.INTEGRATION_MAX_CONVERSATION_MESSAGES;
    private int maxContextCharacters = MemoryConfigurationDefaults.INTEGRATION_MAX_CONTEXT_CHARACTERS;
}
