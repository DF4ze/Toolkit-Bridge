package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAgentRecipientResolverTest {

    @Test
    void resolvesDirectAgentWithoutRoleSelection() {
        AgentRuntimeDirectory directory = mock(AgentRuntimeDirectory.class);
        AgentRoleSelectionStrategy selectionStrategy = mock(AgentRoleSelectionStrategy.class);
        DefaultAgentRecipientResolver resolver = new DefaultAgentRecipientResolver(directory, selectionStrategy);

        AgentRuntime runtime = mock(AgentRuntime.class);
        when(runtime.agentId()).thenReturn("agent-1");
        when(runtime.role()).thenReturn(AgentRole.ASSISTANT);
        when(directory.findByAgentId("agent-1")).thenReturn(Optional.of(runtime));

        Optional<ResolvedRecipient> resolved = resolver.resolve(AgentRecipient.forAgent("agent-1"));

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().agentId()).isEqualTo("agent-1");
    }

    @Test
    void resolvesRoleUsingSelectionStrategy() {
        AgentRuntimeDirectory directory = mock(AgentRuntimeDirectory.class);
        AgentRoleSelectionStrategy selectionStrategy = mock(AgentRoleSelectionStrategy.class);
        DefaultAgentRecipientResolver resolver = new DefaultAgentRecipientResolver(directory, selectionStrategy);

        AgentRuntime candidateA = mock(AgentRuntime.class);
        AgentRuntime candidateB = mock(AgentRuntime.class);
        AgentRuntime selected = mock(AgentRuntime.class);
        when(selected.agentId()).thenReturn("agent-selected");
        when(selected.role()).thenReturn(AgentRole.ASSISTANT);

        when(directory.findByRole(AgentRole.ASSISTANT)).thenReturn(List.of(candidateA, candidateB));
        when(selectionStrategy.select(List.of(candidateA, candidateB))).thenReturn(Optional.of(selected));

        Optional<ResolvedRecipient> resolved = resolver.resolve(AgentRecipient.forRole(AgentRole.ASSISTANT));

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().agentId()).isEqualTo("agent-selected");
        assertThat(resolved.orElseThrow().role()).isEqualTo(AgentRole.ASSISTANT);
    }
}

