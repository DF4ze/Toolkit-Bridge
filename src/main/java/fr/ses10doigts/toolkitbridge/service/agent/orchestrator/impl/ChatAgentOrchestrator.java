package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgentOrchestrator implements AgentOrchestrator {

    private static final int MAX_USER_MESSAGE_LENGTH = 8_000;
    private static final int MAX_LLM_RESPONSE_LENGTH = 20_000;

    private final LlmService llmService;
    private final ConversationMemoryService conversationMemoryService;
    private final ContextAssembler contextAssembler;
    private final EpisodicMemoryService episodicMemoryService;
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

        String projectId = extractProjectId(request);
        try {
            appendUserMessage(memoryKey, request, traceId);

            String context = contextAssembler.buildContext(new ContextRequest(
                    agentId,
                    conversationId,
                    projectId,
                    request.message()
            ));
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
                recordEpisodeFailure(agentId, conversationId, "empty_response");
                return AgentResponse.error("The agent returned an empty response.");
            }

            appendAssistantMessage(memoryKey, request, safeResponse, traceId);
            recordEpisodeSuccess(agentId, conversationId, safeResponse);

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

            recordEpisodeFailure(agentId, conversationId, "provider_failure");
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
            recordEpisodeFailure(agentId, conversationId, "orchestration_error");
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

    private void appendUserMessage(ConversationMemoryKey key, AgentRequest request, String traceId) {
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                key.agentId(),
                key.conversationId(),
                ConversationRole.USER,
                request.message(),
                Instant.now(),
                buildMetadata(request, traceId)
        );
        conversationMemoryService.appendMessage(key, message);
    }

    private void appendAssistantMessage(ConversationMemoryKey key,
                                        AgentRequest request,
                                        String response,
                                        String traceId) {
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                key.agentId(),
                key.conversationId(),
                ConversationRole.ASSISTANT,
                response,
                Instant.now(),
                buildMetadata(request, traceId)
        );
        conversationMemoryService.appendMessage(key, message);
    }

    private Map<String, Object> buildMetadata(AgentRequest request, String traceId) {
        Map<String, Object> metadata = new HashMap<>();
        if (request != null) {
            putIfNotBlank(metadata, "channelType", request.channelType());
            putIfNotBlank(metadata, "channelUserId", request.channelUserId());
            putIfNotBlank(metadata, "channelConversationId", request.channelConversationId());
        }
        putIfNotBlank(metadata, "projectId", extractProjectId(request));
        if (traceId != null && !traceId.isBlank()) {
            metadata.put("traceId", traceId);
        }
        if (request != null && request.context() != null && !request.context().isEmpty()) {
            metadata.put("context", request.context());
        }
        return metadata.isEmpty() ? Map.of() : Map.copyOf(metadata);
    }

    private String extractProjectId(AgentRequest request) {
        if (request == null) {
            return null;
        }
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

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void recordEpisodeSuccess(String agentId, String conversationId, String response) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(agentId);
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId(conversationId);
        event.setType(EpisodeEventType.RESULT);
        event.setAction("agent_exchange");
        event.setDetails("response_length=" + (response == null ? 0 : response.length()));
        event.setStatus(EpisodeStatus.SUCCESS);
        recordEpisodeSafely(event);
    }

    private void recordEpisodeFailure(String agentId, String conversationId, String reason) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(agentId);
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId(conversationId);
        event.setType(EpisodeEventType.ERROR);
        event.setAction("agent_exchange_failed");
        event.setDetails(reason);
        event.setStatus(EpisodeStatus.FAILURE);
        recordEpisodeSafely(event);
    }

    private void recordEpisodeSafely(EpisodeEvent event) {
        try {
            episodicMemoryService.record(event);
        } catch (Exception e) {
            log.warn("Failed to record episodic event for agent={}", event.getAgentId(), e);
        }
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
