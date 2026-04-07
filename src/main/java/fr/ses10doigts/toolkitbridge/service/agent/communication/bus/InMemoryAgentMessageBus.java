package fr.ses10doigts.toolkitbridge.service.agent.communication.bus;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessagePayload;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.AgentRecipientResolver;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.ResolvedRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import fr.ses10doigts.toolkitbridge.service.auth.AgentContextHolder;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class InMemoryAgentMessageBus implements AgentMessageBus {

    private static final String INTERNAL_CHANNEL = "internal";

    private final AgentRecipientResolver recipientResolver;
    private final AgentAccountService agentAccountService;
    private final AgentContextHolder agentContextHolder;
    private final AgentPermissionControlService permissionControlService;

    @Autowired
    public InMemoryAgentMessageBus(AgentRecipientResolver recipientResolver,
                                   AgentAccountService agentAccountService,
                                   AgentContextHolder agentContextHolder,
                                   AgentPermissionControlService permissionControlService) {
        this.recipientResolver = recipientResolver;
        this.agentAccountService = agentAccountService;
        this.agentContextHolder = agentContextHolder;
        this.permissionControlService = permissionControlService;
    }

    @Override
    public AgentMessageDispatchResult dispatch(AgentMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        try {
            permissionControlService.checkDelegation(message.senderAgentId(), message.recipient().kind().name());
        } catch (Exception e) {
            return AgentMessageDispatchResult.failed(
                    message.messageId(),
                    message.correlationId(),
                    null,
                    "Delegation denied"
            );
        }

        Optional<ResolvedRecipient> resolved = recipientResolver.resolve(message.recipient());
        if (resolved.isEmpty()) {
            return AgentMessageDispatchResult.unroutable(
                    message.messageId(),
                    message.correlationId(),
                    "No agent resolved for recipient"
            );
        }

        AgentRuntime runtime = resolved.get().runtime();
        AgentRequest request = buildRequest(message, runtime.agentId());
        AuthenticatedAgent authenticatedTarget;
        try {
            authenticatedTarget = agentAccountService.authenticateByAgentIdent(runtime.agentId());
        } catch (Exception e) {
            log.warn("Message bus authentication failed messageId={} correlationId={} recipientAgentId={}",
                    message.messageId(),
                    message.correlationId(),
                    runtime.agentId(),
                    e);
            return AgentMessageDispatchResult.failed(
                    message.messageId(),
                    message.correlationId(),
                    runtime.agentId(),
                    "Target agent authentication failed"
            );
        }

        runtime.state().startExecution(
                message.correlationId(),
                request.channelType(),
                request.channelConversationId(),
                "orchestrator:" + runtime.orchestrator().getType().name().toLowerCase(),
                request.channelConversationId()
        );
        agentContextHolder.setCurrentBot(authenticatedTarget);

        try {
            AgentResponse response = runtime.orchestrator().handle(runtime, request);
            AgentResponse safeResponse = normalizeResponse(response);
            return AgentMessageDispatchResult.delivered(
                    message.messageId(),
                    message.correlationId(),
                    runtime.agentId(),
                    safeResponse
            );
        } catch (Exception e) {
            log.warn("Message bus dispatch failed messageId={} correlationId={} recipientAgentId={}",
                    message.messageId(),
                    message.correlationId(),
                    runtime.agentId(),
                    e);
            return AgentMessageDispatchResult.failed(
                    message.messageId(),
                    message.correlationId(),
                    runtime.agentId(),
                    "Dispatch execution failed"
            );
        } finally {
            runtime.state().finishExecution();
            agentContextHolder.clear();
        }
    }

    private AgentRequest buildRequest(AgentMessage message, String resolvedAgentId) {
        AgentMessagePayload payload = message.payload();
        Map<String, Object> context = new HashMap<>(payload.context());
        context.put("traceId", message.correlationId());
        context.put("messageId", message.messageId());
        context.put("correlationId", message.correlationId());
        context.put("senderAgentId", message.senderAgentId());
        context.put("messageType", message.type().name());
        context.put("recipientKind", message.recipient().kind().name());
        context.put("artifactRefs", payload.artifacts()
                .stream()
                .map(ref -> Map.of(
                        "artifactId", ref.artifactId(),
                        "type", ref.type().key()
                ))
                .toList());

        return new AgentRequest(
                resolvedAgentId,
                safeChannelType(payload.channelType()),
                payload.channelUserId(),
                payload.channelConversationId(),
                payload.projectId(),
                payload.text(),
                Map.copyOf(context)
        );
    }

    private AgentResponse normalizeResponse(AgentResponse response) {
        if (response == null) {
            return AgentResponse.error("Empty agent response");
        }
        if (response.message() == null || response.message().isBlank()) {
            return AgentResponse.error("Empty agent response");
        }
        return response;
    }

    private String safeChannelType(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            return INTERNAL_CHANNEL;
        }
        return channelType.trim();
    }
}
