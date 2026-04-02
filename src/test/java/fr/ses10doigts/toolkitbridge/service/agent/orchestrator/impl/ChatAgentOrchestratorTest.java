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
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAgentOrchestratorTest {

    private final LlmService llmService = mock(LlmService.class);
    private final MemoryFacade memoryFacade = mock(MemoryFacade.class);
    private final LlmDebugStore llmDebugStore = mock(LlmDebugStore.class);

    private final ChatAgentOrchestrator orchestrator = new ChatAgentOrchestrator(
            llmService,
            memoryFacade,
            llmDebugStore
    );

    @Test
    void runsMemoryFlowBeforeAndAfterLlm() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of("traceId", "t-1"));

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of(10L)));
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true)))
                .thenReturn("assistant response");

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isFalse();
        assertThat(response.message()).isEqualTo("assistant response");

        InOrder inOrder = inOrder(memoryFacade, llmService);
        inOrder.verify(memoryFacade).onUserMessage(any(MemoryContextRequest.class));
        inOrder.verify(memoryFacade).buildContext(any(MemoryContextRequest.class));
        inOrder.verify(llmService).chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true));
        inOrder.verify(memoryFacade).onAssistantMessage(any(MemoryContextRequest.class), eq("assistant response"));
        inOrder.verify(memoryFacade).markContextMemoriesUsed(eq(java.util.List.of(10L)));
    }

    @Test
    void propagatesProjectIdToMemoryRequest() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", "project-42", Map.of("traceId", "t-1"));

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of()));
        when(llmService.chat(any(), any(), any(), any(), anyBoolean())).thenReturn("assistant response");

        orchestrator.handle(agentDefinition, request);

        ArgumentCaptor<MemoryContextRequest> captor = ArgumentCaptor.forClass(MemoryContextRequest.class);
        verify(memoryFacade).onUserMessage(captor.capture());
        assertThat(captor.getValue().projectId()).isEqualTo("project-42");
    }

    @Test
    void recordsFailureThroughToolExecution() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of());

        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("CTX", java.util.List.of()));
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true)))
                .thenThrow(new LlmProviderException("boom"));

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isTrue();
        verify(memoryFacade).onToolExecution(any(MemoryContextRequest.class), any(ToolExecutionRecord.class));
    }

    private AgentDefinition agentDefinition() {
        return new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentOrchestratorType.CHAT,
                "provider",
                "model",
                "system",
                true
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
