package fr.ses10doigts.toolkitbridge.service.llm.runtime;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.LlmCapability;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.ModelInfo;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmProviderRegistryRuntimeTest {

    @Test
    void shouldInitializeRuntimeOnApplicationRun() throws Exception {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmProviderRegistryFactory factory = mock(LlmProviderRegistryFactory.class);
        ApplicationArguments arguments = mock(ApplicationArguments.class);
        when(gateway.loadOpenAiLikeProviders()).thenReturn(List.of());
        when(factory.build(List.of())).thenReturn(registryWith("openai"));

        LlmProviderRegistryRuntime runtime = new LlmProviderRegistryRuntime(gateway, factory);
        runtime.run(arguments);

        assertThat(runtime.snapshot().exists("openai")).isTrue();
    }

    @Test
    void shouldKeepPreviousSnapshotWhenReloadFails() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmProviderRegistryFactory factory = mock(LlmProviderRegistryFactory.class);

        List<fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties> firstPayload = List.of(
                new fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties("openai", "http://localhost:11434/v1", "", "gpt-4.1-mini")
        );
        List<fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties> secondPayload = List.of(
                new fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties("broken", " ", "", "gpt-4.1-mini")
        );

        LlmProviderRegistry firstRegistry = registryWith("openai");

        when(gateway.loadOpenAiLikeProviders()).thenReturn(firstPayload, secondPayload);
        when(factory.build(firstPayload)).thenReturn(firstRegistry);
        when(factory.build(secondPayload)).thenThrow(new IllegalStateException("boom"));

        LlmProviderRegistryRuntime runtime = new LlmProviderRegistryRuntime(gateway, factory);
        runtime.reloadFromDatabase();

        assertThatThrownBy(runtime::reloadFromDatabase)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
        assertThat(runtime.snapshot().exists("openai")).isTrue();
    }

    @Test
    void shouldFailWhenSnapshotRequestedBeforeInitialization() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmProviderRegistryFactory factory = mock(LlmProviderRegistryFactory.class);
        LlmProviderRegistryRuntime runtime = new LlmProviderRegistryRuntime(gateway, factory);

        assertThatThrownBy(runtime::snapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");
    }

    private LlmProviderRegistry registryWith(String providerName) {
        return new LlmProviderRegistry(List.of(new FakeProvider(providerName)));
    }

    private record FakeProvider(String name) implements LlmProvider {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<LlmCapability> getCapabilities() {
            return Set.of();
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public List<ModelInfo> listModels() {
            return List.of();
        }
    }
}
