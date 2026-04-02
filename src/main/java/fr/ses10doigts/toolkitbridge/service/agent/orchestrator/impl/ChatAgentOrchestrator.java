package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgentOrchestrator implements AgentOrchestrator {

    private static final int MAX_USER_MESSAGE_LENGTH = 8_000;
    private static final int MAX_LLM_RESPONSE_LENGTH = 20_000;

    private final LlmService llmService;
    private final MemoryFacade memoryFacade;
    private final LlmDebugStore llmDebugStore;

    @Override
    public AgentOrchestratorType getType() {
        return AgentOrchestratorType.CHAT;
    }

    @Override
    public AgentResponse handle(AgentDefinition agentDefinition, AgentRequest request) {
        validate(agentDefinition, request);

        String agentId = resolveAgentId(agentDefinition, request);
        String conversationId = resolveConversationId(request, agentId);
        ConversationMemoryKey memoryKey = new ConversationMemoryKey(agentId, conversationId);

        String traceId = traceId(request);
        long startNanos = System.nanoTime();
        log.info("Orchestrator start traceId={} agentId={} provider={} model={} messageLength={}",
                traceId,
                agentId,
                agentDefinition.llmProvider(),
                agentDefinition.model(),
                request.message().length());
        log.debug("Orchestrator message preview traceId={} text='{}'", traceId, snippet(request.message()));

        try {
            appendUserMessage(memoryKey, request, traceId);

            String projectId = resolveProjectId(request);
            ContextRequest contextRequest = new ContextRequest(
                    agentId,
                    request.channelUserId(),
                    agentDefinition == null ? null : agentDefinition.telegramBotId(),
                    projectId,
                    request.message(),
                    conversationId,
                    null,
                    null,
                    null,
                    null
            );

            String context = memoryFacade.buildContext(contextRequest);
            log.debug("Memory context=\n'{}'", context);

            long llmStartNanos = System.nanoTime();
            String llmResponse = llmService.chat(
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.systemPrompt(),
                    context,
                    agentDefinition.toolsEnabled()
            );
            log.info("LLM response received traceId={} length={} durationMs={}",
                    traceId,
                    llmResponse == null ? 0 : llmResponse.length(),
                    elapsedMs(llmStartNanos));

            llmDebugStore.recordSuccess(
                    agentId,
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.toolsEnabled(),
                    traceId,
                    agentDefinition.systemPrompt(),
                    context,
                    llmResponse
            );

            String safeResponse = normalizeLlmResponse(llmResponse);
            log.debug("LLM response normalized traceId={} length={}",
                    traceId,
                    safeResponse.length());

            if (safeResponse.isBlank()) {
                memoryFacade.onToolExecution(agentId, conversationId, "agent_exchange_failed", "empty_response", EpisodeStatus.FAILURE);
                return AgentResponse.error("The agent returned an empty response.");
            }

            appendAssistantMessage(memoryKey, request, safeResponse, traceId);

            log.info("Orchestrator completed traceId={} durationMs={}", traceId, elapsedMs(startNanos));
            return AgentResponse.success(safeResponse);
        } catch (LlmProviderException e) {
            log.warn("LLM provider failure for agent={} provider={} model={}",
                    agentDefinition.id(),
                    agentDefinition.name(),
                    agentDefinition.model(),
                    e);

            llmDebugStore.recordFailure(
                    agentId,
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.toolsEnabled(),
                    traceId,
                    agentDefinition.systemPrompt(),
                    request.message(),
                    e.getMessage()
            );

            memoryFacade.onToolExecution(agentId, conversationId, "agent_exchange_failed", "provider_failure", EpisodeStatus.FAILURE);
            return AgentResponse.error("The AI service is temporarily unavailable.");
        } catch (Exception e) {
            log.error("Unexpected agent orchestration error for agent={}", agentDefinition.id(), e);
            llmDebugStore.recordFailure(
                    agentId,
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.toolsEnabled(),
                    traceId,
                    agentDefinition.systemPrompt(),
                    request.message(),
                    e.getMessage()
            );
            memoryFacade.onToolExecution(agentId, conversationId, "agent_exchange_failed", "orchestration_error", EpisodeStatus.FAILURE);
            return AgentResponse.error("An unexpected error occurred.");
        } finally {
            log.debug("Orchestrator finished traceId={} durationMs={}", traceId, elapsedMs(startNanos));
        }
    }

    private void validate(AgentDefinition agentDefinition, AgentRequest request) {
        if (agentDefinition == null) {
            throw new AgentException("agentDefinition must not be null");
        }
        if (request == null) {
            throw new AgentException("request must not be null");
        }
        if (agentDefinition.name() == null || agentDefinition.name().isBlank()) {
            throw new AgentException("Agent LLM provider name must not be blank");
        }
        if (agentDefinition.model() == null || agentDefinition.model().isBlank()) {
            throw new AgentException("Agent model must not be blank");
        }
        if (agentDefinition.systemPrompt() == null || agentDefinition.systemPrompt().isBlank()) {
            throw new AgentException("Agent system prompt must not be blank");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new AgentException("Request message must not be blank");
        }
        if (request.message().length() > MAX_USER_MESSAGE_LENGTH) {
            throw new AgentException("Request message is too long");
        }
    }

    private String normalizeLlmResponse(String response) {
        if (response == null) {
            return "";
        }

        String normalized = response.trim();

        if (normalized.length() > MAX_LLM_RESPONSE_LENGTH) {
            normalized = normalized.substring(0, MAX_LLM_RESPONSE_LENGTH);
        }

        return normalized;
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
        if (request == null) {
            throw new AgentException("request must not be null");
        }
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

    private String resolveProjectId(AgentRequest request) {
        if (request == null || request.context() == null) {
            return null;
        }
        Object value = request.context().get("projectId");
        if (value == null) {
            return null;
        }
        String projectId = String.valueOf(value).trim();
        return projectId.isBlank() ? null : projectId;
    }

    private void appendUserMessage(ConversationMemoryKey key, AgentRequest request, String traceId) {
        memoryFacade.onUserMessage(key, request.message(), buildMetadata(request, traceId));
    }

    private void appendAssistantMessage(ConversationMemoryKey key,
                                        AgentRequest request,
                                        String response,
                                        String traceId) {
        memoryFacade.onAssistantMessage(key, response, buildMetadata(request, traceId));
    }

    private Map<String, Object> buildMetadata(AgentRequest request, String traceId) {
        Map<String, Object> metadata = new HashMap<>();
        if (request != null) {
            putIfNotBlank(metadata, "channelType", request.channelType());
            putIfNotBlank(metadata, "channelUserId", request.channelUserId());
            putIfNotBlank(metadata, "channelConversationId", request.channelConversationId());
        }
        if (traceId != null && !traceId.isBlank()) {
            metadata.put("traceId", traceId);
        }
        if (request != null && request.context() != null && !request.context().isEmpty()) {
            metadata.put("context", request.context());
        }
        return metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
    }

    private String traceId(AgentRequest request) {
        if (request == null || request.context() == null) {
            return "n/a";
        }
        Object value = request.context().get("traceId");
        return value == null ? "n/a" : String.valueOf(value);
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String snippet(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 160) {
            return trimmed;
        }
        return trimmed.substring(0, 160) + "...";
    }
}
