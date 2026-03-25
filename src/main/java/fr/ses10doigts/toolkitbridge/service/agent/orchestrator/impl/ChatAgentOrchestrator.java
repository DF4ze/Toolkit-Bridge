package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgentOrchestrator implements AgentOrchestrator {

    private static final int MAX_USER_MESSAGE_LENGTH = 8_000;
    private static final int MAX_LLM_RESPONSE_LENGTH = 20_000;

    private final LlmService llmService;

    @Override
    public AgentOrchestratorType getType() {
        return AgentOrchestratorType.CHAT;
    }

    @Override
    public AgentResponse handle(AgentDefinition agentDefinition, AgentRequest request) {
        validate(agentDefinition, request);

        String traceId = traceId(request);
        long startNanos = System.nanoTime();
        log.info("Orchestrator start traceId={} agentId={} provider={} model={} messageLength={}",
                traceId,
                agentDefinition.id(),
                agentDefinition.llmProvider(),
                agentDefinition.model(),
                request.message().length());
        log.debug("Orchestrator message preview traceId={} text='{}'", traceId, snippet(request.message()));

        try {
            long llmStartNanos = System.nanoTime();
            String llmResponse = llmService.chat(
                    agentDefinition.llmProvider(),
                    agentDefinition.model(),
                    agentDefinition.systemPrompt(),
                    request.message()
            );
            log.info("LLM response received traceId={} length={} durationMs={}",
                    traceId,
                    llmResponse == null ? 0 : llmResponse.length(),
                    elapsedMs(llmStartNanos));

            String safeResponse = normalizeLlmResponse(llmResponse);
            log.debug("LLM response normalized traceId={} length={}",
                    traceId,
                    safeResponse.length());

            if (safeResponse.isBlank()) {
                return AgentResponse.error("The agent returned an empty response.");
            }

            log.info("Orchestrator completed traceId={} durationMs={}", traceId, elapsedMs(startNanos));
            return AgentResponse.success(safeResponse);
        } catch (LlmProviderException e) {
            log.warn("LLM provider failure for agent={} provider={} model={}",
                    agentDefinition.id(),
                    agentDefinition.name(),
                    agentDefinition.model(),
                    e);

            return AgentResponse.error("The AI service is temporarily unavailable.");
        } catch (Exception e) {
            log.error("Unexpected agent orchestration error for agent={}", agentDefinition.id(), e);
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

    private String traceId(AgentRequest request) {
        if (request == null || request.context() == null) {
            return "n/a";
        }
        Object value = request.context().get("traceId");
        return value == null ? "n/a" : String.valueOf(value);
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
