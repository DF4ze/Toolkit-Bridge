package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrchestrationRequestContextFactory {

    private static final int MAX_USER_MESSAGE_LENGTH = 8_000;
    private final AgentPermissionControlService permissionControlService;

    public OrchestrationRequestContextFactory(AgentPermissionControlService permissionControlService) {
        this.permissionControlService = permissionControlService;
    }

    public OrchestrationRequestContext create(AgentRuntime runtime, AgentRequest request) {
        validate(runtime, request);

        AgentDefinition agentDefinition = runtime.definition();
        String agentId = resolveAgentId(agentDefinition, request);

        return new OrchestrationRequestContext(
                agentId,
                resolveConversationId(request, agentId),
                traceId(request),
                extractProjectId(request),
                request.message().trim(),
                permissionControlService.canExposeTools(runtime)
        );
    }

    private void validate(AgentRuntime runtime, AgentRequest request) {
        if (runtime == null) {
            throw new AgentException("runtime must not be null");
        }
        if (request == null) {
            throw new AgentException("request must not be null");
        }

        AgentDefinition agentDefinition = runtime.definition();
        if (agentDefinition == null) {
            throw new AgentException("runtime definition must not be null");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new AgentException("Request message must not be blank");
        }
        if (request.message().length() > MAX_USER_MESSAGE_LENGTH) {
            throw new AgentException("Request message is too long");
        }
    }

    private String resolveAgentId(AgentDefinition agentDefinition, AgentRequest request) {
        if (agentDefinition != null && agentDefinition.id() != null && !agentDefinition.id().isBlank()) {
            return agentDefinition.id();
        }
        if (request != null && request.agentId() != null && !request.agentId().isBlank()) {
            return request.agentId();
        }
        throw new AgentException("agentId must not be blank");
    }

    private String resolveConversationId(AgentRequest request, String agentId) {
        String channelType = safe(request.channelType());
        String channelConversationId = safe(request.channelConversationId());
        String channelUserId = safe(request.channelUserId());

        if (!channelConversationId.isBlank()) {
            return channelType.isBlank()
                    ? channelConversationId
                    : channelType + ":" + channelConversationId;
        }
        if (!channelUserId.isBlank()) {
            return channelType.isBlank()
                    ? channelUserId
                    : channelType + ":" + channelUserId;
        }
        return agentId;
    }

    private String extractProjectId(AgentRequest request) {
        if (request.projectId() != null && !request.projectId().isBlank()) {
            return request.projectId().trim();
        }
        Map<String, Object> context = request.context();
        if (context == null || context.isEmpty()) {
            return null;
        }
        String candidate = asString(context.get("projectId"));
        if (candidate != null) {
            return candidate;
        }
        return asString(context.get("project_id"));
    }

    private String traceId(AgentRequest request) {
        if (request.context() == null) {
            return "n/a";
        }
        Object value = request.context().get("traceId");
        return value == null ? "n/a" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
