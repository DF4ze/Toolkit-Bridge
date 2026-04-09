package fr.ses10doigts.toolkitbridge.config.llm;

import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeMapper;
import fr.ses10doigts.toolkitbridge.service.llm.provider.ProviderHttpExecutor;
import fr.ses10doigts.toolkitbridge.service.llm.runtime.LlmProviderRegistryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiLikeConfiguration {

    @Bean
    public LlmProviderRegistryFactory llmProviderRegistryFactory(
            OpenAiLikeMapper openAiLikeMapper,
            ProviderHttpExecutor providerHttpExecutor
    ) {
        return new LlmProviderRegistryFactory(openAiLikeMapper, providerHttpExecutor);
    }
}
