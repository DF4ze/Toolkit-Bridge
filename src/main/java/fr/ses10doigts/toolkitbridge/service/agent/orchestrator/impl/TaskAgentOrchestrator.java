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
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task.TaskPrompt;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task.TaskPromptBuilder;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.task.service.TaskFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceCorrelationFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceContextHolder;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
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
public class TaskAgentOrchestrator implements AgentOrchestrator {

    private final LlmService llmService;
    private final LlmDebugStore llmDebugStore;
    private final TaskPromptBuilder taskPromptBuilder;
    private final LlmOrchestrationValidator llmValidator;
    private final OrchestrationRequestContextFactory contextFactory;
    private final MemoryRequestFactory memoryRequestFactory;
    private final OrchestrationResponseSanitizer responseSanitizer;
    private final TaskFactory taskFactory;
    private final AdminTaskStore adminTaskStore;
    private final AgentTraceService agentTraceService;
    private final AgentTraceCorrelationFactory traceCorrelationFactory;
    private final AgentTraceContextHolder traceContextHolder;

    @Override
    public AgentOrchestratorType getType() {
        return AgentOrchestratorType.TASK;
    }

    @Override
    public AgentResponse handle(AgentRuntime runtime, AgentRequest request) {
        OrchestrationRequestContext context = contextFactory.create(runtime, request);
        AgentDefinition definition = runtime.definition();
        llmValidator.validate(definition);
        MemoryFacade memoryFacade = runtime.memory();
        long startNanos = System.nanoTime();

        log.info("Task orchestrator start traceId={} agentId={} provider={} model={} objectiveLength={}",
                context.traceId(),
                context.agentId(),
                definition.llmProvider(),
                definition.model(),
                context.userMessage().length());

        MemoryContextRequest memoryRequest = memoryRequestFactory.from(definition, request, context);
        TaskPrompt prompt = null;
        Task task = null;
        AgentTraceCorrelation traceCorrelation = traceCorrelationFactory.fromOrchestration(context, request, null);

        try {
            memoryFacade.onUserMessage(memoryRequest);
            MemoryContext memoryContext = memoryFacade.buildContext(memoryRequest);
            task = taskFactory.createObjectiveTask(
                    context.userMessage(),
                    request.channelUserId(),
                    runtime.agentId(),
                    context.traceId(),
                    request.context()
            );
            adminTaskStore.record(task, request.channelType(), request.channelConversationId(), null);
            task = task.transitionTo(TaskStatus.RUNNING);
            adminTaskStore.record(task, request.channelType(), request.channelConversationId(), null);
            traceCorrelation = traceCorrelationFactory.fromOrchestration(context, request, task);
            agentTraceService.trace(
                    AgentTraceEventType.TASK_STARTED,
                    "task_orchestrator",
                    traceCorrelation,
                    attributes(
                            "entryPoint", task.entryPoint().name(),
                            "objectiveLength", task.objective().length(),
                            "conversationId", context.conversationId(),
                            "projectId", context.projectId()
                    )
            );
            agentTraceService.trace(
                    AgentTraceEventType.CONTEXT_ASSEMBLED,
                    "task_orchestrator",
                    traceCorrelation,
                    attributes(
                            "contextLength", memoryContext.text().length(),
                            "semanticMemoryCount", memoryContext.injectedSemanticMemoryIds().size(),
                            "toolsEnabled", context.toolsEnabled()
                    )
            );

            prompt = taskPromptBuilder.build(runtime, request, context, task, memoryContext);

            long llmStartNanos = System.nanoTime();
            traceContextHolder.setCurrentCorrelation(traceCorrelation);
            String llmResponse;
            try {
                llmResponse = llmService.chat(
                        definition.llmProvider(),
                        definition.model(),
                        prompt.systemPrompt(),
                        prompt.userPrompt(),
                        runtime.toolAccess().exposedToolDefinitions()
                );
            } finally {
                traceContextHolder.clear();
            }
            log.info("Task orchestrator LLM response traceId={} length={} durationMs={}",
                    context.traceId(),
                    llmResponse == null ? 0 : llmResponse.length(),
                    elapsedMs(llmStartNanos));

            llmDebugStore.recordSuccess(
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    runtime.toolAccess().hasExposedTools(),
                    context.traceId(),
                    prompt.systemPrompt(),
                    prompt.userPrompt(),
                    llmResponse
            );

            String safeResponse = responseSanitizer.normalizeAssistantResponse(llmResponse);
            if (safeResponse.isBlank()) {
                agentTraceService.trace(
                        AgentTraceEventType.ERROR,
                        "task_orchestrator",
                        traceCorrelation,
                        attributes(
                                "category", "response",
                                "reason", "empty_response"
                        )
                );
                memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("task_orchestrator", false, "empty_response"));
                if (task != null) {
                    adminTaskStore.record(markFailed(task), request.channelType(), request.channelConversationId(), "empty_response");
                }
                return AgentResponse.error("The task orchestrator returned an empty response.");
            }

            memoryFacade.onAssistantMessage(memoryRequest, safeResponse);
            memoryFacade.markContextMemoriesUsed(memoryContext.injectedSemanticMemoryIds());
            if (task != null) {
                adminTaskStore.record(markDone(task), request.channelType(), request.channelConversationId(), null);
            }
            agentTraceService.trace(
                    AgentTraceEventType.RESPONSE,
                    "task_orchestrator",
                    traceCorrelation,
                    attributes(
                            "responseLength", safeResponse.length(),
                            "durationMs", elapsedMs(startNanos),
                            "success", true
                    )
            );

            log.info("Task orchestrator completed traceId={} durationMs={}", context.traceId(), elapsedMs(startNanos));
            return AgentResponse.success(safeResponse);
        } catch (LlmProviderException e) {
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "task_orchestrator",
                    traceCorrelation,
                    attributes(
                            "category", "provider",
                            "provider", definition.llmProvider(),
                            "model", definition.model(),
                            "reason", safeMessage(e),
                            "durationMs", elapsedMs(startNanos)
                    )
            );
            log.warn("Task orchestrator provider failure traceId={} agentId={} provider={} model={}",
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
                    prompt == null ? definition.systemPrompt() : prompt.systemPrompt(),
                    prompt == null ? request.message() : prompt.userPrompt(),
                    e.getMessage()
            );

            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("task_orchestrator", false, "provider_failure"));
            if (task != null) {
                adminTaskStore.record(markFailed(task), request.channelType(), request.channelConversationId(), safeMessage(e));
            }
            return AgentResponse.error("The AI service is temporarily unavailable.");
        } catch (Exception e) {
            agentTraceService.trace(
                    AgentTraceEventType.ERROR,
                    "task_orchestrator",
                    traceCorrelation,
                    attributes(
                            "category", "orchestration",
                            "reason", safeMessage(e),
                            "durationMs", elapsedMs(startNanos)
                    )
            );
            log.error("Task orchestrator unexpected error traceId={} agentId={}", context.traceId(), context.agentId(), e);

            llmDebugStore.recordFailure(
                    context.agentId(),
                    definition.llmProvider(),
                    definition.model(),
                    runtime.toolAccess().hasExposedTools(),
                    context.traceId(),
                    prompt == null ? definition.systemPrompt() : prompt.systemPrompt(),
                    prompt == null ? request.message() : prompt.userPrompt(),
                    e.getMessage()
            );

            memoryFacade.onToolExecution(memoryRequest, new ToolExecutionRecord("task_orchestrator", false, "orchestration_error"));
            if (task != null) {
                adminTaskStore.record(markFailed(task), request.channelType(), request.channelConversationId(), safeMessage(e));
            }
            return AgentResponse.error("An unexpected error occurred.");
        } finally {
            log.debug("Task orchestrator finished traceId={} durationMs={}", context.traceId(), elapsedMs(startNanos));
        }
    }

    private Task markDone(Task task) {
        if (task == null || task.status() == TaskStatus.DONE) {
            return task;
        }
        return task.transitionTo(TaskStatus.DONE);
    }

    private Task markFailed(Task task) {
        if (task == null || task.status() == TaskStatus.FAILED) {
            return task;
        }
        return task.transitionTo(TaskStatus.FAILED);
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
