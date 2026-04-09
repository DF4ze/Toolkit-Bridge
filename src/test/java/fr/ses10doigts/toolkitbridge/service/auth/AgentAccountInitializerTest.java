package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.mockito.Mockito.*;

class AgentAccountInitializerTest {

    @Test
    void shouldNotDeleteAccountsWhenDefinitionsArePresent() {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentAccountRepository repository = mock(AgentAccountRepository.class);

        AgentDefinitionProperties first = definition("agent-a");
        AgentDefinitionProperties second = definition("agent-b");
        when(gateway.loadAgentDefinitions()).thenReturn(List.of(first, second));
        when(repository.existsByAgentIdent("agent-a")).thenReturn(true);
        when(repository.existsByAgentIdent("agent-b")).thenReturn(true);

        AgentAccountInitializer initializer = new AgentAccountInitializer(
                gateway,
                accountService,
                repository
        );

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(accountService, never()).createAgent(anyString());
    }

    private static AgentDefinitionProperties definition(String id) {
        AgentDefinitionProperties properties = new AgentDefinitionProperties();
        properties.setId(id);
        return properties;
    }
}
