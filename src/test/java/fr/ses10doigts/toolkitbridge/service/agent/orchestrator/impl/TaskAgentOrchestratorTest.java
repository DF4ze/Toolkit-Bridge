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
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.LlmOrchestrationValidator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.MemoryRequestFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationRequestContextFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationResponseSanitizer;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task.TaskPrompt;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task.TaskPromptBuilder;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.agent.task.service.TaskFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceCorrelationFactory;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceContextHolder;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolKind;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TaskAgentOrchestratorTest {

    private final LlmService llmService = mock(LlmService.class);
    private final MemoryFacade memoryFacade = mock(MemoryFacade.class);
    private final LlmDebugStore llmDebugStore = mock(LlmDebugStore.class);
    private final TaskPromptBuilder taskPromptBuilder = mock(TaskPromptBuilder.class);
    private final AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
    private final AgentTraceService agentTraceService = mock(AgentTraceService.class);
    private final AdminTaskStore taskStore = mock(AdminTaskStore.class);

    private final AgentPolicy policy = new AgentPolicy() {
        @Override
        public String name() {
            return "test";
        }

        @Override
        public boolean allowTools(AgentRuntime runtime, AgentRequest request) {
            return true;
        }
    };

    private final TaskAgentOrchestrator orchestrator = new TaskAgentOrchestrator(
            llmService,
            llmDebugStore,
            taskPromptBuilder,
            new LlmOrchestrationValidator(),
            new OrchestrationRequestContextFactory(permissionControlService),
            new MemoryRequestFactory(),
            new OrchestrationResponseSanitizer(),
            new TaskFactory(),
            taskStore,
            agentTraceService,
            new AgentTraceCorrelationFactory(),
            new AgentTraceContextHolder()
    );

    @Test
    void runsTaskFlowWithExtensionPromptBuilder() {
        AgentDefinition definition = agentDefinition();
        AgentRequest request = request("build release checklist", Map.of("traceId", "trace-1"));
        when(permissionControlService.canExposeTools(any())).thenReturn(true);

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of(7L)));
        when(taskPromptBuilder.build(any(), any(), any(), any(), any()))
                .thenReturn(new TaskPrompt("TASK_SYSTEM", "TASK_USER"));
        when(llmService.chat(eq("provider"), eq("model"), eq("TASK_SYSTEM"), eq("TASK_USER"), anyList()))
                .thenReturn("done");

        AgentResponse response = orchestrator.handle(runtime(definition), request);

        assertThat(response.error()).isFalse();
        assertThat(response.message()).isEqualTo("done");
        verify(memoryFacade).onUserMessage(any(MemoryContextRequest.class));
        verify(taskPromptBuilder).build(any(), any(), any(), any(), any());
        verify(memoryFacade).onAssistantMessage(any(MemoryContextRequest.class), eq("done"));
        verify(memoryFacade).markContextMemoriesUsed(eq(java.util.List.of(7L)));
        verify(taskStore, atLeast(3)).record(any(), any(), any(), any());
    }

    @Test
    void recordsFailureWhenProviderFails() {
        AgentDefinition definition = agentDefinition();
        AgentRequest request = request("ship release", Map.of());
        when(permissionControlService.canExposeTools(any())).thenReturn(true);

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of()));
        when(taskPromptBuilder.build(any(), any(), any(), any(), any()))
                .thenReturn(new TaskPrompt("TASK_SYSTEM", "TASK_USER"));
        when(llmService.chat(eq("provider"), eq("model"), eq("TASK_SYSTEM"), eq("TASK_USER"), anyList()))
                .thenThrow(new LlmProviderException("boom"));

        AgentResponse response = orchestrator.handle(runtime(definition), request);

        assertThat(response.error()).isTrue();
        verify(memoryFacade).onToolExecution(any(MemoryContextRequest.class), any(ToolExecutionRecord.class));
        verify(taskStore, atLeast(3)).record(any(), any(), any(), any());
    }

    private AgentDefinition agentDefinition() {
        return new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.TASK,
                "provider",
                "model",
                "system",
                "default",
                true
        );
    }

    private AgentRuntime runtime(AgentDefinition definition) {
        return new AgentRuntime(
                definition,
                orchestrator,
                memoryFacade,
                new AgentToolAccess(
                        true,
                        Set.of("run_command"),
                        List.of(commandDescriptor()),
                        List.of(commandDescriptor())
                ),
                policy,
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState()
        );
    }

    private ToolDescriptor commandDescriptor() {
        return new ToolDescriptor(
                "run_command",
                ToolKind.SCRIPTED,
                ToolCategory.EXECUTION,
                "Run command",
                Map.of(),
                Set.of(),
                ToolRiskLevel.EXECUTION
        );
    }

    private AgentRequest request(String message, Map<String, Object> context) {
        return new AgentRequest(
                "agent-1",
                "telegram",
                "user-1",
                "chat-1",
                null,
                message,
                context
        );
    }
}
