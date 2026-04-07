package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrchestrationRequestContextFactoryTest {

    @Test
    void buildsContextWithoutLlmSpecificValidation() {
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        OrchestrationRequestContextFactory factory = new OrchestrationRequestContextFactory(permissionControlService);

        AgentDefinition definition = new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.TASK,
                "",
                "",
                "",
                "default",
                true
        );

        AgentPolicy policy = new AgentPolicy() {
            @Override
            public String name() {
                return "default";
            }

            @Override
            public boolean allowTools(AgentRuntime runtime, AgentRequest request) {
                return true;
            }
        };

        AgentRuntime runtime = new AgentRuntime(
                definition,
                mock(fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator.class),
                mock(fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade.class),
                new AgentToolAccess(true, Set.of("run_command")),
                policy,
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState()
        );

        AgentRequest request = new AgentRequest(
                "agent-1",
                "telegram",
                "user-1",
                "chat-1",
                null,
                "hello",
                Map.of("traceId", "trace-1")
        );
        when(permissionControlService.canExposeTools(runtime)).thenReturn(true);

        var context = factory.create(runtime, request);

        assertThat(context.agentId()).isEqualTo("agent-1");
        assertThat(context.conversationId()).isEqualTo("telegram:chat-1");
        assertThat(context.toolsEnabled()).isTrue();
    }
}
