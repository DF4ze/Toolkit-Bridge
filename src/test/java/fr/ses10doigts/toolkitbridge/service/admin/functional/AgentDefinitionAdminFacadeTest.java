package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentPolicyProperties;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentAdminPageView;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentDefinitionAdminForm;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AgentDefinitionAdminFacadeTest {

    @Test
    void listsAgentsWithLlmAndTelegramAssociations() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TelegramBotAdminFacade botAdminFacade = mock(TelegramBotAdminFacade.class);

        AgentDefinitionAdminFacade facade = new AgentDefinitionAdminFacade(
                definitionService,
                accountAdminService,
                gateway,
                llmAdminFacade,
                botAdminFacade
        );

        AgentDefinitionProperties definition = new AgentDefinitionProperties();
        definition.setId("assistant-main");
        definition.setName("Assistant");
        definition.setTelegramBotId("bot-main");
        definition.setOrchestratorType("CHAT");
        definition.setLlmProvider("openai");
        definition.setModel("gpt-5");
        definition.setSystemPrompt("system prompt");
        definition.setRole("ASSISTANT");
        definition.setPolicyName("default");

        when(definitionService.findAll()).thenReturn(List.of(fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition.fromProperties(definition)));
        when(accountAdminService.listAgentAccounts()).thenReturn(List.of(
                new AgentAccountAdminService.AgentAccountSummary(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "assistant-main",
                        true,
                        Instant.parse("2026-01-01T00:00:00Z")
                )
        ));

        List<AgentAdminPageView.AgentItem> items = facade.listAgents();

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().llmProvider()).isEqualTo("openai");
        assertThat(items.getFirst().telegramBotId()).isEqualTo("bot-main");
        assertThat(items.getFirst().accountEnabled()).isTrue();
    }

    @Test
    void createsAndUpdatesAgentDefinition() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TelegramBotAdminFacade botAdminFacade = mock(TelegramBotAdminFacade.class);

        AgentDefinitionAdminFacade facade = new AgentDefinitionAdminFacade(
                definitionService,
                accountAdminService,
                gateway,
                llmAdminFacade,
                botAdminFacade
        );

        when(llmAdminFacade.listLlms()).thenReturn(List.of(new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)));
        when(botAdminFacade.listTelegramBots()).thenReturn(List.of(new TelegramBotAdminResponse("bot-main", true, 123L, true, true)));
        when(accountAdminService.listAgentAccounts()).thenReturn(List.of());

        AgentDefinitionProperties existing = new AgentDefinitionProperties();
        existing.setId("assistant-main");
        existing.setName("Assistant");
        existing.setTelegramBotId("bot-main");
        existing.setOrchestratorType("CHAT");
        existing.setLlmProvider("openai");
        existing.setModel("gpt-4.1-mini");
        existing.setSystemPrompt("old prompt");
        existing.setRole("ASSISTANT");
        existing.setPolicyName("default");

        when(gateway.loadAgentDefinitions()).thenReturn(List.of(), List.of(existing));

        AgentDefinitionAdminForm createForm = baseForm();
        createForm.setAgentId("assistant-main");
        AgentAdminPageView.AgentItem created = facade.createAgentDefinition(createForm);

        assertThat(created.agentId()).isEqualTo("assistant-main");
        verify(gateway, times(1)).saveAgentDefinitions(anyList());

        AgentDefinitionAdminForm updateForm = baseForm();
        updateForm.setName("Assistant v2");
        Optional<AgentAdminPageView.AgentItem> updated = facade.updateAgentDefinition("assistant-main", updateForm);

        assertThat(updated).isPresent();
        verify(gateway, times(2)).saveAgentDefinitions(anyList());
    }

    @Test
    void rejectsUnknownAssociationsAndDuplicates() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TelegramBotAdminFacade botAdminFacade = mock(TelegramBotAdminFacade.class);

        AgentDefinitionAdminFacade facade = new AgentDefinitionAdminFacade(
                definitionService,
                accountAdminService,
                gateway,
                llmAdminFacade,
                botAdminFacade
        );

        when(llmAdminFacade.listLlms()).thenReturn(List.of(new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)));
        when(botAdminFacade.listTelegramBots()).thenReturn(List.of(new TelegramBotAdminResponse("bot-main", true, 123L, true, true)));

        AgentDefinitionProperties existing = new AgentDefinitionProperties();
        existing.setId("assistant-main");
        existing.setName("Assistant");
        existing.setTelegramBotId("bot-main");
        existing.setOrchestratorType("CHAT");
        existing.setLlmProvider("openai");
        existing.setModel("gpt-5");
        existing.setSystemPrompt("prompt");
        existing.setRole("ASSISTANT");
        existing.setPolicyName("default");

        when(gateway.loadAgentDefinitions()).thenReturn(List.of(existing));

        AgentDefinitionAdminForm duplicateForm = baseForm();
        duplicateForm.setAgentId("assistant-main");

        assertThatThrownBy(() -> facade.createAgentDefinition(duplicateForm))
                .isInstanceOf(AgentAdminValidationException.class)
                .hasMessageContaining("already exists");

        AgentDefinitionAdminForm badAssociation = baseForm();
        badAssociation.setAgentId("new-agent");
        badAssociation.setLlmProvider("missing-llm");

        assertThatThrownBy(() -> facade.createAgentDefinition(badAssociation))
                .isInstanceOf(AgentAdminValidationException.class)
                .hasMessageContaining("Unknown LLM provider");
    }

    @Test
    void rejectsInvalidEnumValuesBeforePersisting() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TelegramBotAdminFacade botAdminFacade = mock(TelegramBotAdminFacade.class);

        AgentDefinitionAdminFacade facade = new AgentDefinitionAdminFacade(
                definitionService,
                accountAdminService,
                gateway,
                llmAdminFacade,
                botAdminFacade
        );

        when(llmAdminFacade.listLlms()).thenReturn(List.of(new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)));
        when(botAdminFacade.listTelegramBots()).thenReturn(List.of(new TelegramBotAdminResponse("bot-main", true, 123L, true, true)));
        when(gateway.loadAgentDefinitions()).thenReturn(List.of());

        AgentDefinitionAdminForm invalidForm = baseForm();
        invalidForm.setAgentId("assistant-invalid");
        invalidForm.setOrchestratorType("NOT_A_REAL_TYPE");

        assertThatThrownBy(() -> facade.createAgentDefinition(invalidForm))
                .isInstanceOf(AgentAdminValidationException.class)
                .hasMessageContaining("Invalid orchestratorType");

        verify(gateway, never()).saveAgentDefinitions(anyList());
    }

    @Test
    void preservesPolicyDetailsOnUpdateWhenPolicyIsNotEdited() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AdministrableConfigurationGateway gateway = mock(AdministrableConfigurationGateway.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TelegramBotAdminFacade botAdminFacade = mock(TelegramBotAdminFacade.class);

        AgentDefinitionAdminFacade facade = new AgentDefinitionAdminFacade(
                definitionService,
                accountAdminService,
                gateway,
                llmAdminFacade,
                botAdminFacade
        );

        when(llmAdminFacade.listLlms()).thenReturn(List.of(new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)));
        when(botAdminFacade.listTelegramBots()).thenReturn(List.of(new TelegramBotAdminResponse("bot-main", true, 123L, true, true)));
        when(accountAdminService.listAgentAccounts()).thenReturn(List.of());

        AgentPolicyProperties policy = new AgentPolicyProperties();
        policy.setAllowedTools(new LinkedHashSet<>(List.of("web_search", "calculator")));
        policy.setAccessibleMemoryScopes(new LinkedHashSet<>(List.of("AGENT", "USER")));
        policy.setDelegationAllowed(false);
        policy.setSharedWorkspaceWriteAllowed(false);

        AgentDefinitionProperties existing = new AgentDefinitionProperties();
        existing.setId("assistant-main");
        existing.setName("Assistant");
        existing.setTelegramBotId("bot-main");
        existing.setOrchestratorType("CHAT");
        existing.setLlmProvider("openai");
        existing.setModel("gpt-5");
        existing.setSystemPrompt("old prompt");
        existing.setRole("ASSISTANT");
        existing.setPolicyName("strict");
        existing.setToolsEnabled(false);
        existing.setPolicy(policy);

        when(gateway.loadAgentDefinitions()).thenReturn(List.of(existing));

        AgentDefinitionAdminForm updateForm = baseForm();
        updateForm.setPolicyName("strict-v2");

        facade.updateAgentDefinition("assistant-main", updateForm);

        ArgumentCaptor<List<AgentDefinitionProperties>> definitionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(gateway).saveAgentDefinitions(definitionsCaptor.capture());
        List<AgentDefinitionProperties> savedDefinitions = definitionsCaptor.getValue();

        assertThat(savedDefinitions).hasSize(1);
        AgentDefinitionProperties saved = savedDefinitions.getFirst();
        assertThat(saved.getPolicy()).isSameAs(policy);
        assertThat(saved.getPolicy().getAllowedTools()).containsExactly("web_search", "calculator");
        assertThat(saved.getPolicy().getAccessibleMemoryScopes()).containsExactly("AGENT", "USER");
        assertThat(saved.getPolicy().getDelegationAllowed()).isFalse();
        assertThat(saved.getPolicy().getSharedWorkspaceWriteAllowed()).isFalse();
    }

    private static AgentDefinitionAdminForm baseForm() {
        AgentDefinitionAdminForm form = new AgentDefinitionAdminForm();
        form.setAgentId("assistant-main");
        form.setName("Assistant");
        form.setTelegramBotId("bot-main");
        form.setOrchestratorType("CHAT");
        form.setLlmProvider("openai");
        form.setModel("gpt-5");
        form.setSystemPrompt("System prompt");
        form.setRole("ASSISTANT");
        form.setPolicyName("default");
        form.setToolsEnabled(true);
        return form;
    }
}
