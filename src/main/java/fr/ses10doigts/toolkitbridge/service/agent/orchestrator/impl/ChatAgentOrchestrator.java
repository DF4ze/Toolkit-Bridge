package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
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
        String traceId = traceId(request);
        long startNanos = System.nanoTime();

        log.info("Orchestrator start traceId={} agentId={} provider={} model={} messageLength={}",
                traceId,
                agentId,
                agentDefinition.llmProvider(),
                agentDefinition.model(),
                request.message().length());

        MemoryContextRequest memoryRequest = new MemoryContextRequest(
                agentId,
                request.channelUserId(),
                agentDefinition.telegramBotId(),
                extractProjectId(request),
                request.message(),
                conversationId,
                null,
                null,
                null,
                null
        );

        try {
            memoryFacade.onUserMessage(memoryRequest);
            MemoryContext memoryContext = memoryFacade.buildContext(memoryRequest);

            long llmStartNanos = System.nanoTime();
            String llmResponse = llmService.chat(
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.systemPrompt(),
                    memoryContext.text(),
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
                    memoryContext.text(),
                    llmResponse
            );

            String safeResponse = normalizeLlmResponse(llmResponse);
            if (safeResponse.isBlank()) {
                memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("orchestrator", false, "empty_response"));
                return AgentResponse.error("The agent returned an empty response.");
            }

            memoryFacade.onAssistantMessage(memoryRequest, safeResponse);
            memoryFacade.markContextMemoriesUsed(memoryContext.injectedSemanticMemoryIds());

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

            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("orchestrator", false, "provider_failure"));
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
            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("orchestrator", false, "orchestration_error"));
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

    private String traceId(AgentRequest request) {
        if (request == null || request.context() == null) {
            return "n/a";
        }
        Object value = request.context().get("traceId");
        return value == null ? "n/a" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
