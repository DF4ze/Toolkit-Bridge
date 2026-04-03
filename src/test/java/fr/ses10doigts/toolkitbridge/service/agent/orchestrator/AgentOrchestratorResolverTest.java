package fr.ses10doigts.toolkitbridge.service.agent.orchestrator;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentOrchestratorResolverTest {

    @Test
    void resolvesOrchestratorFromAgentDefinitionType() {
        AgentOrchestrator chat = mock(AgentOrchestrator.class);
        AgentOrchestrator task = mock(AgentOrchestrator.class);
        when(chat.getType()).thenReturn(AgentOrchestratorType.CHAT);
        when(task.getType()).thenReturn(AgentOrchestratorType.TASK);

        AgentOrchestratorResolver resolver = new AgentOrchestratorResolver(
                new AgentOrchestratorRegistry(java.util.List.of(chat, task))
        );

        AgentDefinition definition = new AgentDefinition(
                "agent-task",
                "Task Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.TASK,
                "provider",
                "model",
                "system",
                "default",
                true
        );

        AgentOrchestrator resolved = resolver.resolve(definition);
        assertThat(resolved).isEqualTo(task);
    }
}
