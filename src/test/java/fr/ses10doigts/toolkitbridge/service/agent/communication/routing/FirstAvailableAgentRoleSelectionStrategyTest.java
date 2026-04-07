package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FirstAvailableAgentRoleSelectionStrategyTest {

    private final FirstAvailableAgentRoleSelectionStrategy strategy = new FirstAvailableAgentRoleSelectionStrategy();

    @Test
    void selectsFirstNonBusyRuntimeWhenAvailable() {
        AgentRuntime busy = runtime("agent-busy", true);
        AgentRuntime free = runtime("agent-free", false);

        AgentRuntime selected = strategy.select(List.of(busy, free)).orElseThrow();

        assertThat(selected.agentId()).isEqualTo("agent-free");
    }

    @Test
    void fallsBackToFirstCandidateWhenAllBusy() {
        AgentRuntime busyOne = runtime("agent-1", true);
        AgentRuntime busyTwo = runtime("agent-2", true);

        AgentRuntime selected = strategy.select(List.of(busyOne, busyTwo)).orElseThrow();

        assertThat(selected.agentId()).isEqualTo("agent-1");
    }

    private AgentRuntime runtime(String agentId, boolean busy) {
        AgentRuntimeState state = new AgentRuntimeState();
        if (busy) {
            state.startExecution("trace", "internal", "conv", "task", "ctx");
        }
        return new AgentRuntime(
                new AgentDefinition(
                        agentId,
                        "Agent " + agentId,
                        "bot-" + agentId,
                        AgentRole.ASSISTANT,
                        AgentOrchestratorType.CHAT,
                        "provider",
                        "model",
                        "system",
                        "default",
                        true
                ),
                mock(),
                mock(),
                new AgentToolAccess(true, Set.of("run_command")),
                new ResolvedAgentPolicy("default", Set.of("run_command"), EnumSet.allOf(AgentMemoryScope.class), true, true, true, true),
                new AgentWorkspaceScope(null, null),
                state
        );
    }
}
