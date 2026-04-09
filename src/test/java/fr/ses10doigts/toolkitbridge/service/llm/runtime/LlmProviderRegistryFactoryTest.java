package fr.ses10doigts.toolkitbridge.service.llm.runtime;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.service.llm.openai.OpenAiLikeMapper;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.service.llm.provider.ProviderHttpExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LlmProviderRegistryFactoryTest {

    @Test
    void shouldBuildEmptyRegistryWhenNoProviderConfigured() {
        LlmProviderRegistryFactory factory = new LlmProviderRegistryFactory(
                mock(OpenAiLikeMapper.class),
                mock(ProviderHttpExecutor.class)
        );

        LlmProviderRegistry registry = factory.build(List.of());

        assertThat(registry.getAll()).isEmpty();
    }

    @Test
    void shouldRejectNullProviderConfiguration() {
        LlmProviderRegistryFactory factory = new LlmProviderRegistryFactory(
                mock(OpenAiLikeMapper.class),
                mock(ProviderHttpExecutor.class)
        );
        List<OpenAiLikeProperties> providers = new ArrayList<>();
        providers.add(null);

        assertThatThrownBy(() -> factory.build(providers))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("null");
    }

    @Test
    void shouldRejectProviderWithoutBaseUrl() {
        LlmProviderRegistryFactory factory = new LlmProviderRegistryFactory(
                mock(OpenAiLikeMapper.class),
                mock(ProviderHttpExecutor.class)
        );

        assertThatThrownBy(() -> factory.build(List.of(
                new OpenAiLikeProperties("openai", " ", "sk-1", "gpt-4.1-mini")
        )))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void shouldRejectDuplicateProviderNames() {
        LlmProviderRegistryFactory factory = new LlmProviderRegistryFactory(
                mock(OpenAiLikeMapper.class),
                mock(ProviderHttpExecutor.class)
        );

        assertThatThrownBy(() -> factory.build(List.of(
                new OpenAiLikeProperties("openai", "http://localhost:11434/v1", "", "gpt-4.1-mini"),
                new OpenAiLikeProperties("openai", "http://localhost:11434/v1", "", "gpt-4.1")
        )))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("Duplicate provider name");
    }
}
