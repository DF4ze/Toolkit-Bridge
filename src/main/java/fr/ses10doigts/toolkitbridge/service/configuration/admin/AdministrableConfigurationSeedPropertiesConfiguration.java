package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProvidersProperties;
import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import fr.ses10doigts.toolkitbridge.memory.integration.config.MemoryIntegrationProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.scoring.config.MemoryScoringProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OpenAiLikeProvidersProperties.class,
        ContextAssemblerProperties.class,
        GlobalContextProperties.class,
        MemoryRetrievalProperties.class,
        MemoryIntegrationProperties.class,
        MemoryScoringProperties.class
})
public class AdministrableConfigurationSeedPropertiesConfiguration {
}
