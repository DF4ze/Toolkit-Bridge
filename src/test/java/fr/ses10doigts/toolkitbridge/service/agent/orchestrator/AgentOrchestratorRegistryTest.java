package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentOrchestratorRegistryTest {

    @Test
    void resolvesByType() {
        AgentOrchestrator chat = mock(AgentOrchestrator.class);
        AgentOrchestrator task = mock(AgentOrchestrator.class);
        when(chat.getType()).thenReturn(AgentOrchestratorType.CHAT);
        when(task.getType()).thenReturn(AgentOrchestratorType.TASK);

        AgentOrchestratorRegistry registry = new AgentOrchestratorRegistry(java.util.List.of(chat, task));

        assertThat(registry.getByType(AgentOrchestratorType.CHAT)).isEqualTo(chat);
        assertThat(registry.getByType(AgentOrchestratorType.TASK)).isEqualTo(task);
    }

    @Test
    void failsFastWhenTwoOrchestratorsShareType() {
        AgentOrchestrator first = mock(AgentOrchestrator.class);
        AgentOrchestrator second = mock(AgentOrchestrator.class);
        when(first.getType()).thenReturn(AgentOrchestratorType.CHAT);
        when(second.getType()).thenReturn(AgentOrchestratorType.CHAT);

        assertThatThrownBy(() -> new AgentOrchestratorRegistry(java.util.List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate orchestrator registration");
    }
}
