package fr.ses10doigts.toolkitbridge.service.agent.communication.bus;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessagePayload;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessageType;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.AgentRecipientResolver;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.ResolvedRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceCorrelationFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import fr.ses10doigts.toolkitbridge.service.auth.AgentContextHolder;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

class InMemoryAgentMessageBusTest {

    @Test
    void dispatchesMessageToResolvedAgent() {
        AgentRecipientResolver resolver = mock(AgentRecipientResolver.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentContextHolder contextHolder = mock(AgentContextHolder.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        AgentTraceService agentTraceService = mock(AgentTraceService.class);
        doNothing().when(permissionControlService).checkDelegation(any(), any());
        InMemoryAgentMessageBus bus = new InMemoryAgentMessageBus(
                resolver,
                accountService,
                contextHolder,
                permissionControlService,
                agentTraceService,
                new AgentTraceCorrelationFactory()
        );

        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        when(orchestrator.getType()).thenReturn(AgentOrchestratorType.CHAT);
        when(orchestrator.handle(any(), any())).thenReturn(AgentResponse.success("ok"));

        AgentRuntime runtime = runtime("agent-target", orchestrator);
        AgentMessage message = AgentMessage.create(
                "corr-1",
                "agent-sender",
                AgentRecipient.forAgent("agent-target"),
                AgentMessageType.TASK_REQUEST,
                new AgentMessagePayload("build release notes", "internal", "agent-sender", "conv-1", null, Map.of())
        );

        when(resolver.resolve(message.recipient()))
                .thenReturn(Optional.of(new ResolvedRecipient("agent-target", AgentRole.ASSISTANT, runtime)));
        when(accountService.authenticateByAgentIdent("agent-target"))
                .thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "agent-target"));

        AgentMessageDispatchResult result = bus.dispatch(message);

        assertThat(result.status()).isEqualTo(AgentMessageDispatchStatus.DELIVERED);
        assertThat(result.resolvedAgentId()).isEqualTo("agent-target");
        assertThat(result.response()).isNotNull();
        assertThat(result.response().error()).isFalse();
        assertThat(runtime.state().isBusy()).isFalse();
        verify(contextHolder).setCurrentBot(any(AuthenticatedAgent.class));
        verify(contextHolder).clear();
    }

    @Test
    void returnsUnroutableWhenNoRecipientMatches() {
        AgentRecipientResolver resolver = mock(AgentRecipientResolver.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentContextHolder contextHolder = mock(AgentContextHolder.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        AgentTraceService agentTraceService = mock(AgentTraceService.class);
        doNothing().when(permissionControlService).checkDelegation(any(), any());
        InMemoryAgentMessageBus bus = new InMemoryAgentMessageBus(
                resolver,
                accountService,
                contextHolder,
                permissionControlService,
                agentTraceService,
                new AgentTraceCorrelationFactory()
        );

        AgentMessage message = AgentMessage.create(
                "agent-sender",
                AgentRecipient.forRole(AgentRole.OPERATOR),
                AgentMessageType.QUESTION,
                new AgentMessagePayload("who can help?", "internal", null, null, null, Map.of())
        );
        when(resolver.resolve(message.recipient())).thenReturn(Optional.empty());

        AgentMessageDispatchResult result = bus.dispatch(message);

        assertThat(result.status()).isEqualTo(AgentMessageDispatchStatus.UNROUTABLE);
        assertThat(result.resolvedAgentId()).isNull();
    }

    @Test
    void returnsFailedWhenTargetAuthenticationFails() {
        AgentRecipientResolver resolver = mock(AgentRecipientResolver.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentContextHolder contextHolder = mock(AgentContextHolder.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        AgentTraceService agentTraceService = mock(AgentTraceService.class);
        doNothing().when(permissionControlService).checkDelegation(any(), any());
        InMemoryAgentMessageBus bus = new InMemoryAgentMessageBus(
                resolver,
                accountService,
                contextHolder,
                permissionControlService,
                agentTraceService,
                new AgentTraceCorrelationFactory()
        );

        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        when(orchestrator.getType()).thenReturn(AgentOrchestratorType.CHAT);
        AgentRuntime runtime = runtime("agent-target", orchestrator);

        AgentMessage message = AgentMessage.create(
                "agent-sender",
                AgentRecipient.forAgent("agent-target"),
                AgentMessageType.QUESTION,
                new AgentMessagePayload("Can you validate this?", "internal", null, null, null, Map.of())
        );

        when(resolver.resolve(message.recipient()))
                .thenReturn(Optional.of(new ResolvedRecipient("agent-target", AgentRole.ASSISTANT, runtime)));
        when(accountService.authenticateByAgentIdent("agent-target")).thenThrow(new IllegalStateException("boom"));

        AgentMessageDispatchResult result = bus.dispatch(message);

        assertThat(result.status()).isEqualTo(AgentMessageDispatchStatus.FAILED);
        assertThat(result.details()).contains("authentication");
        verify(orchestrator, never()).handle(any(), any());
        verify(contextHolder, never()).setCurrentBot(any());
    }

    private AgentRuntime runtime(String agentId, AgentOrchestrator orchestrator) {
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
                orchestrator,
                mock(),
                new AgentToolAccess(true, Set.of("run_command")),
                new ResolvedAgentPolicy("default", Set.of("run_command"), EnumSet.allOf(fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope.class), true, true, true, true),
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState()
        );
    }
}
