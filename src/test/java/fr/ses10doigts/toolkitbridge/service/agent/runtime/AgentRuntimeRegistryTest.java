package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentRuntimeRegistryTest {

    @Test
    void centralizesRuntimeInstancesByAgentId() {
        AgentRuntimeFactory factory = mock(AgentRuntimeFactory.class);
        AgentRuntimeRegistry registry = new AgentRuntimeRegistry(factory);

        AgentDefinition definition = definition("agent-1", AgentRole.ASSISTANT);
        AuthenticatedAgent authenticatedAgent = new AuthenticatedAgent(UUID.randomUUID(), "agent-1");
        AgentRuntime runtime = mock(AgentRuntime.class);

        when(factory.create(definition, authenticatedAgent)).thenReturn(runtime);
        when(runtime.role()).thenReturn(AgentRole.ASSISTANT);

        AgentRuntime first = registry.getOrCreate(definition, authenticatedAgent);
        AgentRuntime second = registry.getOrCreate(definition, authenticatedAgent);

        assertThat(first).isSameAs(runtime);
        assertThat(second).isSameAs(runtime);
        assertThat(registry.findByAgentId("agent-1")).contains(runtime);
        assertThat(registry.findByRole(AgentRole.ASSISTANT)).containsExactly(runtime);
        verify(factory, times(1)).create(definition, authenticatedAgent);
    }

    private AgentDefinition definition(String id, AgentRole role) {
        return new AgentDefinition(
                id,
                "Agent-" + id,
                "bot-" + id,
                role,
                AgentOrchestratorType.CHAT,
                "provider",
                "model",
                "system",
                "default",
                true
        );
    }
}
