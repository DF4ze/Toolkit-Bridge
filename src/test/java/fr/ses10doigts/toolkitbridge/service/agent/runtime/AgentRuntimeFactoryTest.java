package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentAvailability;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestratorResolver;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestratorRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolKind;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeFactoryTest {

    @Test
    void buildsRuntimeWithExplicitSubsystemContracts() throws Exception {
        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        when(orchestrator.getType()).thenReturn(AgentOrchestratorType.CHAT);

        AgentPolicy policy = mock(AgentPolicy.class);
        when(policy.name()).thenReturn("default");
        when(policy.resolve(eq(definition("agent-1")), any(fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess.class)))
                .thenReturn(new ResolvedAgentPolicy("default", Set.of("run_command", "read_file"), EnumSet.allOf(fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope.class), true, true, true, true));

        MemoryFacade memoryFacade = mock(MemoryFacade.class);
        ToolRegistryService toolRegistry = mock(ToolRegistryService.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        List<ToolDescriptor> descriptors = List.of(
                descriptor("run_command", ToolCategory.EXECUTION, ToolKind.SCRIPTED, ToolRiskLevel.EXECUTION),
                descriptor("read_file", ToolCategory.FILES, ToolKind.NATIVE, ToolRiskLevel.READ_ONLY)
        );
        when(toolRegistry.getToolNames()).thenReturn(Set.of("run_command", "read_file"));
        when(toolRegistry.getToolDescriptors()).thenReturn(descriptors);
        when(toolRegistry.getToolDescriptors(Set.of("run_command", "read_file"))).thenReturn(descriptors);
        when(workspaceService.getAgentWorkspace("agent-1")).thenReturn(Path.of("/tmp/agent-1"));
        when(workspaceService.getSharedWorkspace()).thenReturn(Path.of("/tmp/shared"));

        AgentRuntimeFactory factory = new AgentRuntimeFactory(
                new AgentOrchestratorResolver(new AgentOrchestratorRegistry(java.util.List.of(orchestrator))),
                new AgentPolicyRegistry(java.util.List.of(policy)),
                memoryFacade,
                toolRegistry,
                workspaceService
        );

        AgentDefinition definition = definition("agent-1");

        AuthenticatedAgent authenticatedAgent = new AuthenticatedAgent(UUID.randomUUID(), "agent-1");
        AgentRuntime runtime = factory.create(definition, authenticatedAgent);

        assertThat(runtime.definition()).isEqualTo(definition);
        assertThat(runtime.orchestrator()).isEqualTo(orchestrator);
        assertThat(runtime.memory()).isEqualTo(memoryFacade);
        assertThat(runtime.policy().allowedTools()).contains("run_command", "read_file");
        assertThat(runtime.toolAccess().allowedTools()).contains("run_command", "read_file");
        assertThat(runtime.toolAccess().registeredTools()).hasSize(2);
        assertThat(runtime.toolAccess().exposedTools()).hasSize(2);
        assertThat(runtime.workspace().agentWorkspace()).isEqualTo(Path.of("/tmp/agent-1"));
        assertThat(runtime.workspace().sharedWorkspace()).isEqualTo(Path.of("/tmp/shared"));
        assertThat(runtime.state().snapshot().availability()).isEqualTo(AgentAvailability.AVAILABLE);
        assertThat(runtime.state().snapshot().busy()).isFalse();
    }

    private ToolDescriptor descriptor(String name, ToolCategory category, ToolKind kind, ToolRiskLevel riskLevel) {
        return new ToolDescriptor(name, kind, category, name, java.util.Map.of(), Set.of(), riskLevel);
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
