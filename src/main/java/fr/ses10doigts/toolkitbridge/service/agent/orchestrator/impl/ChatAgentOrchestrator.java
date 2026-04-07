package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

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
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model.OrchestrationRequestContext;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.LlmOrchestrationValidator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.MemoryRequestFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationRequestContextFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationResponseSanitizer;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceCorrelationFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceContextHolder;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgentOrchestrator implements AgentOrchestrator {

    private final LlmService llmService;
    private final LlmDebugStore llmDebugStore;
    private final LlmOrchestrationValidator llmValidator;
    private final OrchestrationRequestContextFactory contextFactory;
    private final MemoryRequestFactory memoryRequestFactory;
    private final OrchestrationResponseSanitizer responseSanitizer;
    private final AgentTraceService agentTraceService;
    private final AgentTraceCorrelationFactory traceCorrelationFactory;
    private final AgentTraceContextHolder traceContextHolder;

    @Override
    public AgentOrchestratorType getType() {
        return AgentOrchestratorType.CHAT;
    }

    @Override
    public AgentResponse handle(AgentRuntime runtime, AgentRequest request) {
        OrchestrationRequestContext context = contextFactory.create(runtime, request);
        AgentDefinition definition = runtime.definition();
        llmValidator.validate(definition);
        MemoryFacade memoryFacade = runtime.memory();
        long startNanos = System.nanoTime();
        AgentTraceCorrelation traceCorrelation = traceCorrelationFactory.fromOrchestration(context, request, null);

        log.info("Chat orchestrator start traceId={} agentId={} provider={} model={} messageLength={}",
                context.traceId(),
                context.agentId(),
                definition.llmProvider(),
                definition.model(),
                context.userMessage().length());

        MemoryContextRequest memoryRequest = memoryRequestFactory.from(definition, request, context);

        try {
            memoryFacade.onUserMessage(memoryRequest);
            MemoryContext memoryContext = memoryFacade.buildContext(memoryRequest);
            agentTraceService.trace(
                    AgentTraceEventType.CONTEXT_ASSEMBLED,
                    "chat_orchestrator",
                    traceCorrelation,
                    attributes(
                            "conversationId", context.conversationId(),
                            "projectId", context.projectId(),
                            "contextLength", memoryContext.text().length(),
                            "semanticMemoryCount", memoryContext.injectedSemanticMemoryIds().size(),
                            "toolsEnabled", context.toolsEnabled()
                    )
            );

            long llmStartNanos = System.nanoTime();
            traceContextHolder.setCurrentCorrelation(traceCorrelation);
            String llmResponse;
            try {
                llmResponse = llmService.chat(
                        definition.llmProvider(),
                        definition.model(),
                        definition.systemPrompt(),
                        memoryContext.text(),
                        runtime.toolAccess().exposedToolDefinitions()
                );
            } finally {
                traceContextHolder.clear();
            }
            log.info("Chat orchestrator LLM response traceId={} length={} durationMs={}",
                    context.traceId(),
                    llmResponse == null ? 0 : llmResponse.length(),
                    elapsedMs(llmStartNanos));

            llmDebugStore.recordSuccess(
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    runtime.toolAccess().hasExposedTools(),
                    context.traceId(),
                    definition.systemPrompt(),
                    memoryContext.text(),
                    llmResponse
            );

            String safeResponse = responseSanitizer.normalizeAssistantResponse(llmResponse);
            if (safeResponse.isBlank()) {
                agentTraceService.trace(
                        AgentTraceEventType.ERROR,
                        "chat_orchestrator",
                        traceCorrelation,
                        attributes(
                                "category", "response",
                                "reason", "empty_response"
                        )
                );
                memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("chat_orchestrator", false, "empty_response"));
                return AgentResponse.error("The agent returned an empty response.");
            }

            memoryFacade.onAssistantMessage(memoryRequest, safeResponse);
            memoryFacade.markContextMemoriesUsed(memoryContext.injectedSemanticMemoryIds());
            agentTraceService.trace(
                    AgentTraceEventType.RESPONSE,
                    "chat_orchestrator",
                    traceCorrelation,
                    attributes(
                            "responseLength", safeResponse.length(),
                            "durationMs", elapsedMs(startNanos),
                            "success", true
                    )
            );

            log.info("Chat orchestrator completed traceId={} durationMs={}", context.traceId(), elapsedMs(startNanos));
            return AgentResponse.success(safeResponse);
        } catch (LlmProviderException e) {
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "chat_orchestrator",
                    traceCorrelation,
                    attributes(
                            "category", "provider",
                            "provider", definition.llmProvider(),
                            "model", definition.model(),
                            "reason", safeMessage(e),
                            "durationMs", elapsedMs(startNanos)
                    )
            );
            log.warn("Chat orchestrator provider failure traceId={} agentId={} provider={} model={}",
                    context.traceId(),
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    e);

            llmDebugStore.recordFailure(
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    runtime.toolAccess().hasExposedTools(),
                    context.traceId(),
                    definition.systemPrompt(),
                    request.message(),
                    e.getMessage()
            );

            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("chat_orchestrator", false, "provider_failure"));
            return AgentResponse.error("The AI service is temporarily unavailable.");
        } catch (Exception e) {
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "chat_orchestrator",
                    traceCorrelation,
                    attributes(
                            "category", "orchestration",
                            "reason", safeMessage(e),
                            "durationMs", elapsedMs(startNanos)
                    )
            );
            log.error("Chat orchestrator unexpected error traceId={} agentId={}", context.traceId(), context.agentId(), e);

            llmDebugStore.recordFailure(
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    runtime.toolAccess().hasExposedTools(),
                    context.traceId(),
                    definition.systemPrompt(),
                    request.message(),
                    e.getMessage()
            );

            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("chat_orchestrator", false, "orchestration_error"));
            return AgentResponse.error("An unexpected error occurred.");
        } finally {
            log.debug("Chat orchestrator finished traceId={} durationMs={}", context.traceId(), elapsedMs(startNanos));
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String safeMessage(Throwable e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return e == null ? "unknown error" : e.getClass().getSimpleName();
        }
        return e.getMessage();
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
