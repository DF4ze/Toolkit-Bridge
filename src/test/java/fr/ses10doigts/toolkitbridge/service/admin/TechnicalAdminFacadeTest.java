package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceQueryService;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TechnicalAdminFacadeTest {

    @Test
    void configurationViewDoesNotExposeApiKeyValue() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeRegistry runtimeRegistry = mock(AgentRuntimeRegistry.class);
        AgentPolicyRegistry policyRegistry = mock(AgentPolicyRegistry.class);
        ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);
        AgentTraceQueryService traceSink = mock(AgentTraceQueryService.class);
        AdminTaskStore taskStore = mock(AdminTaskStore.class);
        ArtifactService artifactService = mock(ArtifactService.class);
        AdministrableConfigurationGateway configurationGateway = mock(AdministrableConfigurationGateway.class);
        PersistenceRetentionPolicyResolver retentionPolicyResolver = mock(PersistenceRetentionPolicyResolver.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();

        when(definitionService.findAll()).thenReturn(List.of(new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.CHAT,
                "openai",
                "gpt-4.1-mini",
                "system",
                "default",
                true
        )));
        when(configurationGateway.loadOpenAiLikeProviders()).thenReturn(List.of(
                new OpenAiLikeProperties("openai", "https://api.openai.com/v1", "sk-secret", "gpt-4.1-mini")
        ));

        TechnicalAdminFacade facade = new TechnicalAdminFacade(
                definitionService,
                runtimeRegistry,
                policyRegistry,
                toolRegistryService,
                traceSink,
                taskStore,
                artifactService,
                configurationGateway,
                retentionPolicyResolver,
                properties
        );

        TechnicalAdminView.ConfigItem config = facade.getConfigurationView();

        assertThat(config.agentDefinitionCount()).isEqualTo(1);
        assertThat(config.llmProviderCount()).isEqualTo(1);
        assertThat(config.llmProviders()).hasSize(1);
        assertThat(config.llmProviders().get(0).apiKeyConfigured()).isTrue();
        assertThat(config.llmProviders().get(0).name()).isEqualTo("openai");
        assertThat(config.llmProviders().get(0).baseUrl()).isEqualTo("https://api.openai.com/v1");
    }
}
