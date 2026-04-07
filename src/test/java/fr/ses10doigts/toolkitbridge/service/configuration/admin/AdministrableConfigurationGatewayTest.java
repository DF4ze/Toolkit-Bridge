package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.agent.AgentsProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProvidersProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdministrableConfigurationGatewayTest {

    @Test
    void shouldPreferDatabaseAgentDefinitionsOverYamlSeed() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AgentsProperties agentsProperties = new AgentsProperties();
        agentsProperties.setDefinitions(List.of(agent("yaml-agent")));
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();

        List<AgentDefinitionProperties> dbDefinitions = List.of(agent("db-agent"));
        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.of(dbDefinitions));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(
                storeService,
                agentsProperties,
                providersProperties
        );

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).extracting(AgentDefinitionProperties::getId).containsExactly("db-agent");
    }

    @Test
    void shouldFallbackToYamlSeedWhenDatabaseConfigIsMissing() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AgentsProperties agentsProperties = new AgentsProperties();
        agentsProperties.setDefinitions(List.of(agent("yaml-agent")));
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();

        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.empty());

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(
                storeService,
                agentsProperties,
                providersProperties
        );

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).extracting(AgentDefinitionProperties::getId).containsExactly("yaml-agent");
    }

    @Test
    void shouldRespectDatabaseValueWhenItIsExplicitlyEmpty() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AgentsProperties agentsProperties = new AgentsProperties();
        agentsProperties.setDefinitions(List.of(agent("yaml-agent")));
        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();

        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of()));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(
                storeService,
                agentsProperties,
                providersProperties
        );

        List<AgentDefinitionProperties> resolved = gateway.loadAgentDefinitions();

        assertThat(resolved).isEmpty();
    }

    @Test
    void bootstrapShouldSeedOnlyMissingKeys() {
        AdministrableConfigurationStoreService storeService = mock(AdministrableConfigurationStoreService.class);
        AgentsProperties agentsProperties = new AgentsProperties();
        agentsProperties.setDefinitions(List.of(agent("seed-agent")));

        OpenAiLikeProvidersProperties providersProperties = new OpenAiLikeProvidersProperties();
        providersProperties.setProviders(List.of(new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "", "gpt-4.1-mini")));

        when(storeService.read(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(TypeReference.class)))
                .thenReturn(Optional.empty());
        when(storeService.read(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(TypeReference.class)))
                .thenReturn(Optional.of(List.of(new OpenAiLikeProperties("existing", "http://localhost", "", "model"))));

        AdministrableConfigurationGateway gateway = new AdministrableConfigurationGateway(
                storeService,
                agentsProperties,
                providersProperties
        );

        boolean seeded = gateway.bootstrapSeedsIfMissing();

        assertThat(seeded).isTrue();
        verify(storeService, times(1)).write(eq(AdministrableConfigKey.AGENT_DEFINITIONS), any(List.class));
        verify(storeService, never()).write(eq(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS), any(List.class));
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
