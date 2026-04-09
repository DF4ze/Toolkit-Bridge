package fr.ses10doigts.toolkitbridge.service.llm.runtime;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeMapper;
import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.service.llm.provider.ProviderHttpExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class LlmProviderRegistryFactory {

    private final OpenAiLikeMapper openAiLikeMapper;
    private final ProviderHttpExecutor providerHttpExecutor;

    public LlmProviderRegistryFactory(OpenAiLikeMapper openAiLikeMapper,
                                      ProviderHttpExecutor providerHttpExecutor) {
        this.openAiLikeMapper = openAiLikeMapper;
        this.providerHttpExecutor = providerHttpExecutor;
    }

    public LlmProviderRegistry build(List<OpenAiLikeProperties> providerPropertiesList) {
        List<OpenAiLikeProperties> safeProviderProperties = providerPropertiesList == null
                ? List.of()
                : new ArrayList<>(providerPropertiesList);

        log.info("Configured OpenAI-like providers: {}",
                safeProviderProperties.stream()
                        .map(properties -> properties == null ? "<null>" : properties.name())
                        .collect(Collectors.toList()));

        if (safeProviderProperties.isEmpty()) {
            log.warn("No OpenAI-like providers configured in DB; building an empty provider registry");
            return new LlmProviderRegistry(List.of());
        }

        List<LlmProvider> providers = safeProviderProperties.stream()
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
