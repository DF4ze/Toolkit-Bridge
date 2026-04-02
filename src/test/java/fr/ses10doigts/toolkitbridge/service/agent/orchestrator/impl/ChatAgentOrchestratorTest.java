package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
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
    void writesConversationMemoryAndCallsContextBeforeLlm() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of("traceId", "t-1"));

        when(memoryFacade.buildContext(any(ContextRequest.class))).thenReturn("CTX");
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true)))
                .thenReturn("assistant response");

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isFalse();
        assertThat(response.message()).isEqualTo("assistant response");

        ArgumentCaptor<String> userContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> userMetadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(memoryFacade).onUserMessage(any(), userContentCaptor.capture(), userMetadataCaptor.capture());
        assertThat(userContentCaptor.getValue()).isEqualTo("hello");
        assertThat(userMetadataCaptor.getValue()).containsEntry("traceId", "t-1");

        ArgumentCaptor<String> assistantContentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> assistantMetadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(memoryFacade).onAssistantMessage(any(), assistantContentCaptor.capture(), assistantMetadataCaptor.capture());
        assertThat(assistantContentCaptor.getValue()).isEqualTo("assistant response");

        ArgumentCaptor<ContextRequest> contextCaptor = ArgumentCaptor.forClass(ContextRequest.class);
        verify(memoryFacade).buildContext(contextCaptor.capture());
        ContextRequest contextRequest = contextCaptor.getValue();
        assertThat(contextRequest.agentId()).isEqualTo("agent-1");
        assertThat(contextRequest.userId()).isEqualTo("user-1");
        assertThat(contextRequest.botId()).isEqualTo("bot-1");
        assertThat(contextRequest.conversationId()).isEqualTo("telegram:chat-1");
        assertThat(contextRequest.projectId()).isNull();
        assertThat(contextRequest.currentUserMessage()).isEqualTo("hello");

        InOrder inOrder = inOrder(memoryFacade, llmService);
        inOrder.verify(memoryFacade).onUserMessage(any(), any(), any());
        inOrder.verify(memoryFacade).buildContext(any());
        inOrder.verify(llmService).chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true));
        inOrder.verify(memoryFacade).onAssistantMessage(any(), any(), any());
    }

    @Test
    void recordsEpisodicFailureAndPropagatesError() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of());

        when(memoryFacade.buildContext(any(ContextRequest.class))).thenReturn("CTX");
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true)))
                .thenThrow(new LlmProviderException("boom"));

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isTrue();
        assertThat(response.message()).isNotBlank();

        verify(memoryFacade).onUserMessage(any(), eq("hello"), any());
        verify(memoryFacade, never()).onAssistantMessage(any(), any(), any());

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EpisodeStatus> statusCaptor = ArgumentCaptor.forClass(EpisodeStatus.class);
        verify(memoryFacade).onToolExecution(any(), any(), actionCaptor.capture(), detailsCaptor.capture(), statusCaptor.capture());
        assertThat(actionCaptor.getValue()).isEqualTo("agent_exchange_failed");
        assertThat(detailsCaptor.getValue()).isEqualTo("provider_failure");
        assertThat(statusCaptor.getValue()).isEqualTo(EpisodeStatus.FAILURE);
    }

    @Test
    void forwardsProjectIdFromContext() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of("projectId", "project-42"));

        when(memoryFacade.buildContext(any(ContextRequest.class))).thenReturn("CTX");
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"), eq(true)))
                .thenReturn("assistant response");

        orchestrator.handle(agentDefinition, request);

        ArgumentCaptor<ContextRequest> contextCaptor = ArgumentCaptor.forClass(ContextRequest.class);
        verify(memoryFacade).buildContext(contextCaptor.capture());
        assertThat(contextCaptor.getValue().projectId()).isEqualTo("project-42");
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
        return new AgentRequest(
                "agent-1",
                "telegram",
                "user-1",
                "chat-1",
                message,
                context
        );
    }
}
