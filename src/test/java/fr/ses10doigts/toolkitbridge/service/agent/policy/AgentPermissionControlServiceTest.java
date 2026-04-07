package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.exception.AgentPermissionDeniedException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolSecurityDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentPermissionControlServiceTest {

    @Test
    void canExposeToolsUsesResolvedRuntimePolicy() {
        AgentPermissionControlService service = new AgentPermissionControlService(
                mock(CurrentAgentService.class),
                mock(AgentDefinitionService.class),
                mock(AgentPolicyRegistry.class),
                new StaticListableBeanFactory(java.util.Map.of(
                        "toolRegistryService",
                        mock(ToolRegistryService.class)
                )).getBeanProvider(ToolRegistryService.class)
        );

        AgentRuntime runtime = new AgentRuntime(
                definition("agent-1"),
                mock(fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator.class),
                mock(fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade.class),
                new AgentToolAccess(true, Set.of("run_command")),
                new ResolvedAgentPolicy(
                        "default",
                        Set.of(),
                        EnumSet.allOf(AgentMemoryScope.class),
                        true,
                        true,
                        true,
                        true
                ),
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState()
        );

        assertThat(service.canExposeTools(runtime)).isFalse();
    }

    @Test
    void checkToolExecutionRejectsScriptedToolWhenPolicyDisablesScriptedExecution() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentPolicy policy = mock(AgentPolicy.class);
        AgentPolicyRegistry policyRegistry = new AgentPolicyRegistry(java.util.List.of(policy));
        ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);
        ObjectProvider<ToolRegistryService> toolRegistryProvider =
                new StaticListableBeanFactory(java.util.Map.of("toolRegistryService", toolRegistryService))
                        .getBeanProvider(ToolRegistryService.class);

        when(definitionService.findById("agent-1")).thenReturn(Optional.of(definition("agent-1")));
        when(policy.name()).thenReturn("default");
        when(policy.resolve(definition("agent-1"), new AgentToolAccess(true, Set.of("run_command"))))
                .thenReturn(new ResolvedAgentPolicy(
                        "default",
                        Set.of("run_command"),
                        EnumSet.allOf(AgentMemoryScope.class),
                        true,
                        true,
                        true,
                        false
                ));
        when(toolRegistryService.getToolNames()).thenReturn(Set.of("run_command"));
        when(toolRegistryService.getSecurityDescriptor("run_command")).thenReturn(ToolSecurityDescriptor.scripted());

        AgentPermissionControlService service = new AgentPermissionControlService(
                mock(CurrentAgentService.class),
                definitionService,
                policyRegistry,
                toolRegistryProvider
        );

        assertThatThrownBy(() -> service.checkToolExecution("agent-1", "run_command"))
                .isInstanceOf(AgentPermissionDeniedException.class)
                .hasMessageContaining("SCRIPTED_TOOL_EXECUTION");
    }

    private AgentDefinition definition(String agentId) {
        return new AgentDefinition(
                agentId,
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.CHAT,
                "provider",
                "model",
                "system",
                "default",
                true
        );
    }
}
