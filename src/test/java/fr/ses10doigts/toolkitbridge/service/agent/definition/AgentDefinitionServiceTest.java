package fr.ses10doigts.toolkitbridge.service.agent.definition;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentDefinitionServiceTest {

    @Test
    void shouldResolveDefinitionsFromGatewayAtCallTime() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        when(gateway.loadAgentDefinitions())
                .thenReturn(List.of(props("first")))
                .thenReturn(List.of(props("second")));

        AgentDefinitionService service = new AgentDefinitionService(gateway);

        List<AgentDefinition> firstRead = service.findAll();
        List<AgentDefinition> secondRead = service.findAll();

        assertThat(firstRead).extracting(AgentDefinition::id).containsExactly("first");
        assertThat(secondRead).extracting(AgentDefinition::id).containsExactly("second");
        verify(gateway, times(2)).loadAgentDefinitions();
    }

    private static AgentDefinitionProperties props(String id) {
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

