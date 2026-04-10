package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminConfigQueryServiceTest {

    @Test
    void getConfigurationViewCountsAndMapsProvidersWithoutExposingApiKey() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AdministrableConfigurationGateway configurationGateway = mock(AdministrableConfigurationGateway.class);
        when(definitionService.findAll()).thenReturn(List.of(
                definition("agent-1", AgentRole.ASSISTANT),
                definition("agent-2", AgentRole.EXECUTOR)
        ));
        when(configurationGateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "sk-secret", "gpt-4.1-mini"),
                new OpenAiLikeProperties("local", "http://localhost:8080/v1", "   ", "llama-3")
        ));

        AdminConfigQueryService service = new AdminConfigQueryService(definitionService, configurationGateway);

        TechnicalAdminView.ConfigItem item = service.getConfigurationView();

        assertThat(item.agentDefinitionCount()).isEqualTo(2);
        assertThat(item.llmProviderCount()).isEqualTo(2);
        assertThat(item.llmProviders()).hasSize(2);

        TechnicalAdminView.LlmProviderItem first = item.llmProviders().get(0);
        assertThat(first.name()).isEqualTo("openai");
        assertThat(first.baseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(first.defaultModel()).isEqualTo("gpt-4.1-mini");
        assertThat(first.apiKeyConfigured()).isTrue();

        TechnicalAdminView.LlmProviderItem second = item.llmProviders().get(1);
        assertThat(second.name()).isEqualTo("local");
        assertThat(second.baseUrl()).isEqualTo("http://localhost:8080/v1");
        assertThat(second.defaultModel()).isEqualTo("llama-3");
        assertThat(second.apiKeyConfigured()).isFalse();
    }

    private AgentDefinition definition(String id, AgentRole role) {
        return new AgentDefinition(
                id,
                "Agent " + id,
                "bot-" + id,
                role,
                AgentOrchestratorType.CHAT,
                "openai",
                "gpt-4.1-mini",
                "system",
                "default",
                true
        );
    }
}
