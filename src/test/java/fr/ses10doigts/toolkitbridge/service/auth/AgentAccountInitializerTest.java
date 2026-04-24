package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import fr.ses10doigts.toolkitbridge.security.SensitiveDataMasker;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldLogMaskedApiKeyWhenCreatingMissingAccount(CapturedOutput output) {
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentAccountRepository repository = mock(AgentAccountRepository.class);

        AgentDefinitionProperties first = definition("agent-a");
        when(gateway.loadAgentDefinitions()).thenReturn(List.of(first));
        when(repository.existsByAgentIdent("agent-a")).thenReturn(false);
        String rawApiKey = "tb_abcdefgh.abcdefghijklmnopqrstuvwxyz123456";
        when(accountService.createAgent("agent-a")).thenReturn(new AgentProvisioningResult("agent-a", rawApiKey));

        AgentAccountInitializer initializer = new AgentAccountInitializer(
                gateway,
                accountService,
                repository
        );

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(output.getAll()).doesNotContain(rawApiKey);
        assertThat(output.getAll()).contains(SensitiveDataMasker.mask(rawApiKey));
    }

    private static AgentDefinitionProperties definition(String id) {
        AgentDefinitionProperties properties = new AgentDefinitionProperties();
        properties.setId(id);
        return properties;
    }
}
