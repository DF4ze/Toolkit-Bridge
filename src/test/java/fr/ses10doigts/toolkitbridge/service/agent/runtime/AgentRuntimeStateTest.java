package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentAvailability;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRuntimeStateTest {

    @Test
    void tracksMinimalExecutionLifecycle() {
        AgentRuntimeState state = new AgentRuntimeState();

        assertThat(state.snapshot().availability()).isEqualTo(AgentAvailability.AVAILABLE);
        assertThat(state.snapshot().busy()).isFalse();

        state.startExecution("trace-1", "telegram", "conv-1", "orchestrator:chat", "conv-1");

        assertThat(state.snapshot().busy()).isTrue();
        assertThat(state.snapshot().currentTask()).isEqualTo("orchestrator:chat");
        assertThat(state.snapshot().activeContext()).isEqualTo("conv-1");
        assertThat(state.snapshot().traceId()).isEqualTo("trace-1");

        state.finishExecution();

        assertThat(state.snapshot().busy()).isFalse();
        assertThat(state.snapshot().currentTask()).isNull();
        assertThat(state.snapshot().activeContext()).isNull();
    }
}
