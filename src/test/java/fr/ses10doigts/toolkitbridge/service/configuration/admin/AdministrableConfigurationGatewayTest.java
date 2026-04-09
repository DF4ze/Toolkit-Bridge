package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministrableConfigurationGatewayTest {

    @Test
    void shouldPreferDatabaseAgentDefinitionsOverYamlSeed() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);

        List<AgentDefinitionProperties> dbDefinitions = List.of(agent("db-agent"));
        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.of(dbDefinitions));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).extracting(AgentDefinitionProperties::getId).containsExactly("db-agent");
    }

    @Test
    void shouldReturnEmptyWhenDatabaseAgentDefinitionsAreMissing() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);

        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).isEmpty();
    }

    @Test
    void shouldRespectDatabaseValueWhenItIsExplicitlyEmpty() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);

        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of()));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDatabaseOpenAiProvidersAreMissing() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);

        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        List<OpenAiLikeProperties> resolved = gateway.loadOpenAiLikeProviders();

        assertThat(resolved).isEmpty();
    }

    @Test
    void shouldLoadMemoryConfigurationFromDatabaseWhenPresent() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();
        payload.getContext().setMaxCharacters(42);

        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.of(payload));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        Optional<MemoryConfigurationPayload> resolved = gateway.loadMemoryConfiguration();

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getContext().getMaxCharacters()).isEqualTo(42);
    }

    @Test
    void shouldSaveMemoryConfigurationThroughStore() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();
        payload.getConversation().setEnabled(Boolean.TRUE);

        gateway.saveMemoryConfiguration(payload);

        verify(storeService, times(1)).write(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), eq(payload));
    }

    @Test
    void shouldRejectNullMemoryConfigurationPayload() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        assertThatThrownBy(() -> gateway.saveMemoryConfiguration(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memory configuration payload must not be null");
        verify(storeService, never()).write(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any());
    }

    @Test
    void shouldReturnEmptyOptionalWhenMemoryConfigurationIsMissing() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        when(storeService.read(eq(AdministrableConfigKey.MEMORY_CONFIGURATION), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(storeService);

        Optional<MemoryConfigurationPayload> resolved = gateway.loadMemoryConfiguration();

        assertThat(resolved).isEmpty();
    }

    private static AgentDefinitionProperties agent(String id) {
        AgentDefinitionProperties properties = new AgentDefinitionProperties();
        properties.setId(id);
        properties.setName(id);
        properties.setTelegramBotId(id + "-bot");
        properties.setOrchestratorType("CHAT");
        properties.setLlmProvider("openai");
        properties.setModel("gpt-4.1-mini");
        properties.setSystemPrompt("system");
        return properties;
    }
}
