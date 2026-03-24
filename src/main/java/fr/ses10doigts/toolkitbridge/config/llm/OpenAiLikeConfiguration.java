package fr.ses10doigts.toolkitbridge.config.llm;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.service.llm.provider.ProviderHttpExecutor;
import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeMapper;
import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@EnableConfigurationProperties(OpenAiLikeProvidersProperties.class)
public class OpenAiLikeConfiguration {

    @Bean
    public LlmProviderRegistry llmProviderRegistry(
            OpenAiLikeProvidersProperties openAiLikeProvidersProperties,
            OpenAiLikeMapper openAiLikeMapper,
            ProviderHttpExecutor providerHttpExecutor
    ) {
        List<OpenAiLikeProperties> providerPropertiesList = openAiLikeProvidersProperties.getProviders();

        log.info("providerPropertiesList: {}", providerPropertiesList);

        if (providerPropertiesList == null || providerPropertiesList.isEmpty()) {
            throw new LlmProviderException("No OpenAI-like providers configured");
        }

        List<LlmProvider> providers = providerPropertiesList.stream()
                .map(this::validateProviderProperties)
                .map(properties -> new OpenAiLikeProvider(
                        properties,
                        openAiLikeMapper,
                        providerHttpExecutor
                ))
                .map(LlmProvider.class::cast)
                .toList();

        return new LlmProviderRegistry(providers);
    }

    private OpenAiLikeProperties validateProviderProperties(OpenAiLikeProperties properties) {
        if (properties == null) {
            throw new LlmProviderException("A provider configuration is null");
        }

        if (properties.name() == null || properties.name().isBlank()) {
            throw new LlmProviderException("A provider has no name");
        }

        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new LlmProviderException(
                    "Provider '%s' has no baseUrl".formatted(properties.name())
            );
        }

        return properties;
    }
}