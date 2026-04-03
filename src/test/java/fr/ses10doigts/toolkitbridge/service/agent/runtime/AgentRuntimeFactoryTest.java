package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestratorRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRuntimeFactoryTest {

    @Test
    void buildsRuntimeWithExplicitSubsystemContracts() throws Exception {
        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        when(orchestrator.getType()).thenReturn(AgentOrchestratorType.CHAT);

        AgentPolicy policy = mock(AgentPolicy.class);
        when(policy.name()).thenReturn("default");

        MemoryFacade memoryFacade = mock(MemoryFacade.class);
        ToolRegistryService toolRegistry = mock(ToolRegistryService.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        when(toolRegistry.getToolNames()).thenReturn(Set.of("run_command", "read_file"));
        when(workspaceService.getAgentWorkspace("agent-1")).thenReturn(Path.of("/tmp/agent-1"));
        when(workspaceService.getSharedWorkspace()).thenReturn(Path.of("/tmp/shared"));

        AgentRuntimeFactory factory = new AgentRuntimeFactory(
                new AgentOrchestratorRegistry(java.util.List.of(orchestrator)),
                new AgentPolicyRegistry(java.util.List.of(policy)),
                memoryFacade,
                toolRegistry,
                workspaceService
        );

        AgentDefinition definition = new AgentDefinition(
                "agent-1",
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

        AuthenticatedAgent authenticatedAgent = new AuthenticatedAgent(UUID.randomUUID(), "agent-1");
        AgentRuntimeState state = new AgentRuntimeState("trace", "telegram", "user", "conv", Instant.now());

        AgentRuntime runtime = factory.create(definition, authenticatedAgent, state);

        assertThat(runtime.definition()).isEqualTo(definition);
        assertThat(runtime.orchestrator()).isEqualTo(orchestrator);
        assertThat(runtime.memory()).isEqualTo(memoryFacade);
        assertThat(runtime.policy()).isEqualTo(policy);
        assertThat(runtime.toolAccess().allowedTools()).contains("run_command", "read_file");
        assertThat(runtime.workspace().agentWorkspace()).isEqualTo(Path.of("/tmp/agent-1"));
        assertThat(runtime.workspace().sharedWorkspace()).isEqualTo(Path.of("/tmp/shared"));
        assertThat(runtime.state()).isEqualTo(state);
    }
}
