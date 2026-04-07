package fr.ses10doigts.toolkitbridge.service.agent.communication.bus;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessagePayload;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.AgentRecipientResolver;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.communication.routing.ResolvedRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceCorrelationFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import fr.ses10doigts.toolkitbridge.service.auth.AgentContextHolder;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final AgentTraceService agentTraceService;
    private final AgentTraceCorrelationFactory traceCorrelationFactory;

    @Autowired
    public InMemoryAgentMessageBus(AgentRecipientResolver recipientResolver,
                                   AgentAccountService agentAccountService,
                                   AgentContextHolder agentContextHolder,
                                   AgentPermissionControlService permissionControlService,
                                   AgentTraceService agentTraceService,
                                   AgentTraceCorrelationFactory traceCorrelationFactory) {
        this.recipientResolver = recipientResolver;
        this.agentAccountService = agentAccountService;
        this.agentContextHolder = agentContextHolder;
        this.permissionControlService = permissionControlService;
        this.agentTraceService = agentTraceService;
        this.traceCorrelationFactory = traceCorrelationFactory;
    }

    @Override
    public AgentMessageDispatchResult dispatch(AgentMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        try {
            permissionControlService.checkDelegation(message.senderAgentId(), message.recipient().kind().name());
        } catch (Exception e) {
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "message_bus",
                    traceCorrelationFactory.fromMessage(message, null),
                    attributes(
                            "category", "delegation",
                            "status", "denied",
                            "recipientKind", message.recipient().kind().name(),
                            "reason", "Delegation denied"
                    )
            );
            return AgentMessageDispatchResult.failed(
                    message.messageId(),
                    message.correlationId(),
                    null,
                    "Delegation denied"
            );
        }

        Optional<ResolvedRecipient> resolved = recipientResolver.resolve(message.recipient());
        if (resolved.isEmpty()) {
            agentTraceService.trace(
                    AgentTraceEventType.DELEGATION,
                    "message_bus",
                    traceCorrelationFactory.fromMessage(message, null),
                    attributes(
                            "status", "unroutable",
                            "recipientKind", message.recipient().kind().name()
                    )
            );
            return AgentMessageDispatchResult.unroutable(
                    message.messageId(),
                    message.correlationId(),
                    "No agent resolved for recipient"
            );
        }

        AgentRuntime runtime = resolved.get().runtime();
        AgentTraceCorrelation traceCorrelation = traceCorrelationFactory.fromMessage(message, runtime.agentId());
        agentTraceService.trace(
                AgentTraceEventType.DELEGATION,
                "message_bus",
                traceCorrelation,
                attributes(
                        "status", "resolved",
                        "recipientKind", message.recipient().kind().name(),
                        "messageType", message.type().name(),
                        "senderAgentId", message.senderAgentId(),
                        "resolvedAgentId", runtime.agentId()
                )
        );
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
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "message_bus",
                    traceCorrelation,
                    attributes(
                            "category", "delegation",
                            "status", "authentication_failed",
                            "resolvedAgentId", runtime.agentId(),
                            "reason", "Target agent authentication failed"
                    )
            );
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
            agentTraceService.trace(
                    AgentTraceEventType.DELEGATION,
                    "message_bus",
                    traceCorrelation,
                    attributes(
                            "status", "delivered",
                            "resolvedAgentId", runtime.agentId(),
                            "responseError", safeResponse.error(),
                            "responseLength", safeResponse.message() == null ? 0 : safeResponse.message().length()
                    )
            );
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
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "message_bus",
                    traceCorrelation,
                    attributes(
                            "category", "delegation",
                            "status", "dispatch_failed",
                            "resolvedAgentId", runtime.agentId(),
                            "reason", "Dispatch execution failed"
                    )
            );
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

    private Map<String, Object> attributes(Object... values) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object key = values[i];
            if (!(key instanceof String stringKey) || stringKey.isBlank()) {
                continue;
            }
            Object value = values[i + 1];
            if (value != null) {
                attributes.put(stringKey, value);
            }
        }
        return Map.copyOf(attributes);
    }
}
