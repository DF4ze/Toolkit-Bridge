package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultAgentPolicyTest {

    private final DefaultAgentPolicy policy = new DefaultAgentPolicy();

    @Test
    void allowsToolsOnlyWhenRuntimeAndDefinitionEnableIt() {
        AgentRequest request = new AgentRequest("agent-1", "telegram", "user", "conv", null, "hello", Map.of());

        AgentRuntime allowedRuntime = runtime(true, true);
        AgentRuntime deniedByDefinition = runtime(false, true);
        AgentRuntime deniedByRuntimeAccess = runtime(true, false);

        assertThat(policy.allowTools(allowedRuntime, request)).isTrue();
        assertThat(policy.allowTools(deniedByDefinition, request)).isFalse();
        assertThat(policy.allowTools(deniedByRuntimeAccess, request)).isFalse();
    }

    private AgentRuntime runtime(boolean definitionToolsEnabled, boolean runtimeToolsEnabled) {
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
                definitionToolsEnabled
        );

        return new AgentRuntime(
                definition,
                mock(AgentOrchestrator.class),
                mock(fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade.class),
                new AgentToolAccess(runtimeToolsEnabled, Set.of("run_command")),
                policy,
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState("trace", "telegram", "user", "conv", Instant.now())
        );
    }
}
