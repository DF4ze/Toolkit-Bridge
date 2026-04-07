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
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolKind;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAgentOrchestratorTest {

    private final LlmService llmService = mock(LlmService.class);
    private final MemoryFacade memoryFacade = mock(MemoryFacade.class);
    private final LlmDebugStore llmDebugStore = mock(LlmDebugStore.class);
    private final AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
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

    private final ChatAgentOrchestrator orchestrator = new ChatAgentOrchestrator(
            llmService,
            llmDebugStore,
            new LlmOrchestrationValidator(),
            new OrchestrationRequestContextFactory(permissionControlService),
            new MemoryRequestFactory(),
            new OrchestrationResponseSanitizer()
    );

    @Test
    void runsMemoryFlowBeforeAndAfterLlm() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of("traceId", "t-1"));
        when(permissionControlService.canExposeTools(any())).thenReturn(true);

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of(10L)));
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), anyList()))
                .thenReturn("assistant response");

        AgentResponse response = orchestrator.handle(runtime(agentDefinition), request);

        assertThat(response.error()).isFalse();
        assertThat(response.message()).isEqualTo("assistant response");

        InOrder inOrder = inOrder(memoryFacade, llmService);
        inOrder.verify(memoryFacade).onUserMessage(any(MemoryContextRequest.class));
        inOrder.verify(memoryFacade).buildContext(any(MemoryContextRequest.class));
        inOrder.verify(llmService).chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), anyList());
        inOrder.verify(memoryFacade).onAssistantMessage(any(MemoryContextRequest.class), eq("assistant response"));
        inOrder.verify(memoryFacade).markContextMemoriesUsed(eq(java.util.List.of(10L)));
    }

    @Test
    void propagatesProjectIdToMemoryRequest() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", "project-42", Map.of("traceId", "t-1"));
        when(permissionControlService.canExposeTools(any())).thenReturn(true);

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of()));
        when(llmService.chat(any(), any(), any(), any(), anyList())).thenReturn("assistant response");

        orchestrator.handle(runtime(agentDefinition), request);

        ArgumentCaptor<MemoryContextRequest> captor = ArgumentCaptor.forClass(MemoryContextRequest.class);
        verify(memoryFacade).onUserMessage(captor.capture());
        assertThat(captor.getValue().projectId()).isEqualTo("project-42");
    }

    @Test
    void recordsFailureThroughToolExecution() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of());
        when(permissionControlService.canExposeTools(any())).thenReturn(true);

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of()));
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), anyList()))
                .thenThrow(new LlmProviderException("boom"));

        AgentResponse response = orchestrator.handle(runtime(agentDefinition), request);

        assertThat(response.error()).isTrue();
        verify(memoryFacade).onToolExecution(any(MemoryContextRequest.class), any(ToolExecutionRecord.class));
    }

    private AgentDefinition agentDefinition() {
        return new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.CHAT,
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
        return request(message, null, context);
    }

    private AgentRequest request(String message, String projectId, Map<String, Object> context) {
        return new AgentRequest(
                "agent-1",
                "telegram",
                "user-1",
                "chat-1",
                projectId,
                message,
                context
        );
    }
}
