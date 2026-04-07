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
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

            long llmStartNanos = System.nanoTime();
            String llmResponse = llmService.chat(
                    definition.llmProvider(),
                    definition.model(),
                    definition.systemPrompt(),
                    memoryContext.text(),
                    runtime.toolAccess().exposedToolDefinitions()
            );
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
                memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("chat_orchestrator", false, "empty_response"));
                return AgentResponse.error("The agent returned an empty response.");
            }

            memoryFacade.onAssistantMessage(memoryRequest, safeResponse);
            memoryFacade.markContextMemoriesUsed(memoryContext.injectedSemanticMemoryIds());

            log.info("Chat orchestrator completed traceId={} durationMs={}", context.traceId(), elapsedMs(startNanos));
            return AgentResponse.success(safeResponse);
        } catch (LlmProviderException e) {
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
}
