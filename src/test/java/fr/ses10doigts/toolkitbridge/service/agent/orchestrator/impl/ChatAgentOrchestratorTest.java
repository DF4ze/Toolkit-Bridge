package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAgentOrchestratorTest {

    private final LlmService llmService = mock(LlmService.class);
    private final ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
    private final ContextAssembler contextAssembler = mock(ContextAssembler.class);
    private final EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);

    private final ChatAgentOrchestrator orchestrator = new ChatAgentOrchestrator(
            llmService,
            conversationMemoryService,
            contextAssembler,
            episodicMemoryService
    );

    @Test
    void writesConversationMemoryAndCallsContextBeforeLlm() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of("traceId", "t-1"));

        when(contextAssembler.buildContext(any(ContextRequest.class))).thenReturn("CTX");
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX"))).thenReturn("assistant response");

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isFalse();
        assertThat(response.message()).isEqualTo("assistant response");

        ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(conversationMemoryService, times(2)).appendMessage(any(), messageCaptor.capture());

        List<ConversationMessage> messages = messageCaptor.getAllValues();
        assertThat(messages.get(0).role()).isEqualTo(ConversationRole.USER);
        assertThat(messages.get(0).content()).isEqualTo("hello");
        assertThat(messages.get(1).role()).isEqualTo(ConversationRole.ASSISTANT);
        assertThat(messages.get(1).content()).isEqualTo("assistant response");

        ArgumentCaptor<ContextRequest> contextCaptor = ArgumentCaptor.forClass(ContextRequest.class);
        verify(contextAssembler).buildContext(contextCaptor.capture());
        ContextRequest contextRequest = contextCaptor.getValue();
        assertThat(contextRequest.agentId()).isEqualTo("agent-1");
        assertThat(contextRequest.conversationId()).isEqualTo("telegram:chat-1");
        assertThat(contextRequest.projectId()).isNull();
        assertThat(contextRequest.userMessage()).isEqualTo("hello");

        InOrder inOrder = inOrder(conversationMemoryService, contextAssembler, llmService, episodicMemoryService);
        inOrder.verify(conversationMemoryService).appendMessage(any(), any());
        inOrder.verify(contextAssembler).buildContext(any());
        inOrder.verify(llmService).chat(eq("provider"), eq("model"), eq("system"), eq("CTX"));
        inOrder.verify(conversationMemoryService).appendMessage(any(), any());
        inOrder.verify(episodicMemoryService).record(any(EpisodeEvent.class));
    }

    @Test
    void recordsEpisodicFailureAndPropagatesError() {
        AgentDefinition agentDefinition = agentDefinition();
        AgentRequest request = request("hello", Map.of());

        when(contextAssembler.buildContext(any(ContextRequest.class))).thenReturn("CTX");
        when(llmService.chat(eq("provider"), eq("model"), eq("system"), eq("CTX")))
                .thenThrow(new LlmProviderException("boom"));

        AgentResponse response = orchestrator.handle(agentDefinition, request);

        assertThat(response.error()).isTrue();
        assertThat(response.message()).isNotBlank();

        ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(conversationMemoryService, atLeastOnce()).appendMessage(any(), messageCaptor.capture());
        assertThat(messageCaptor.getAllValues())
                .extracting(ConversationMessage::role)
                .containsOnly(ConversationRole.USER);

        ArgumentCaptor<EpisodeEvent> eventCaptor = ArgumentCaptor.forClass(EpisodeEvent.class);
        verify(episodicMemoryService).record(eventCaptor.capture());
        EpisodeEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo(EpisodeEventType.ERROR);
        assertThat(event.getStatus()).isEqualTo(EpisodeStatus.FAILURE);
    }

    private AgentDefinition agentDefinition() {
        return new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentOrchestratorType.CHAT,
                "provider",
                "model",
                "system"
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
