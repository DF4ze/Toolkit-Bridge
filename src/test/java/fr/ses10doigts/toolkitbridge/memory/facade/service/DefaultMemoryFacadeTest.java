package fr.ses10doigts.toolkitbridge.memory.facade.service;

import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.factory.EpisodicEventFactory;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
import fr.ses10doigts.toolkitbridge.memory.integration.config.MemoryIntegrationProperties;
import fr.ses10doigts.toolkitbridge.memory.integration.service.ImplicitMemoryWritePipeline;
import fr.ses10doigts.toolkitbridge.memory.retrieval.facade.MemoryRetrievalFacade;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultMemoryFacadeTest {

    @Test
    void buildContextUsesRetrieverAndAssembler() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ContextAssembler assembler = mock(ContextAssembler.class);
        DefaultMemoryFacade facade = facade(
                retrievalFacade,
                assembler,
                mock(ConversationMemoryService.class),
                mock(EpisodicMemoryService.class),
                mock(SemanticMemoryService.class),
                mock(ImplicitMemoryWritePipeline.class),
                mock(EpisodicEventFactory.class),
                new MemoryIntegrationProperties()
        );

        RetrievedMemories memories = new RetrievedMemories(List.of(), List.of(), List.of(), "conv");
        when(retrievalFacade.retrieve(any(ContextRequest.class))).thenReturn(memories);
        when(assembler.buildContext(any(ContextRequest.class), any(RetrievedMemories.class)))
                .thenReturn(new AssembledContext("CTX", List.of(10L, 12L)));

        MemoryContext context = facade.buildContext(request("hello"));

        assertThat(context.text()).isEqualTo("CTX");
        assertThat(context.injectedSemanticMemoryIds()).containsExactly(10L, 12L);
        verify(retrievalFacade).retrieve(any(ContextRequest.class));
        verify(assembler).buildContext(any(ContextRequest.class), any(RetrievedMemories.class));
    }

    @Test
    void buildContextPropagatesRequestOverridesToContextRequest() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ContextAssembler assembler = mock(ContextAssembler.class);
        DefaultMemoryFacade facade = facade(
                retrievalFacade,
                assembler,
                mock(ConversationMemoryService.class),
                mock(EpisodicMemoryService.class),
                mock(SemanticMemoryService.class),
                mock(ImplicitMemoryWritePipeline.class),
                mock(EpisodicEventFactory.class),
                new MemoryIntegrationProperties()
        );

        when(retrievalFacade.retrieve(any(ContextRequest.class)))
                .thenReturn(new RetrievedMemories(List.of(), List.of(), List.of(), ""));
        when(assembler.buildContext(any(ContextRequest.class), any(RetrievedMemories.class)))
                .thenReturn(new AssembledContext("CTX", List.of()));

        MemoryContextRequest request = new MemoryContextRequest(
                "agent-1", "user-1", "bot-1", "project-1", "hello", "conv-1", 2, 1, null, null
        );
        facade.buildContext(request);

        ArgumentCaptor<ContextRequest> contextCaptor = ArgumentCaptor.forClass(ContextRequest.class);
        verify(retrievalFacade).retrieve(contextCaptor.capture());
        assertThat(contextCaptor.getValue().maxSemanticMemories()).isEqualTo(2);
        assertThat(contextCaptor.getValue().maxEpisodes()).isEqualTo(1);
    }

    @Test
    void onUserAndAssistantMessagesTriggerWriters() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ContextAssembler assembler = mock(ContextAssembler.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        SemanticMemoryService semanticMemoryService = mock(SemanticMemoryService.class);
        ImplicitMemoryWritePipeline implicitMemoryWritePipeline = mock(ImplicitMemoryWritePipeline.class);
        EpisodicEventFactory eventFactory = mock(EpisodicEventFactory.class);
        MemoryIntegrationProperties properties = new MemoryIntegrationProperties();
        when(eventFactory.userMessageReceived(any())).thenReturn(new EpisodeEvent());
        when(eventFactory.assistantResponseGenerated(any(), any())).thenReturn(new EpisodeEvent());

        DefaultMemoryFacade facade = facade(
                retrievalFacade,
                assembler,
                conversationMemoryService,
                episodicMemoryService,
                semanticMemoryService,
                implicitMemoryWritePipeline,
                eventFactory,
                properties
        );

        facade.onUserMessage(request("hello"));
        facade.onAssistantMessage(request("hello"), "assistant");

        ArgumentCaptor<fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage> captor =
                ArgumentCaptor.forClass(fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage.class);
        verify(conversationMemoryService, times(2)).appendMessage(any(), captor.capture());
        assertThat(captor.getAllValues()).extracting(fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage::role)
                .containsExactly(ConversationRole.USER, ConversationRole.ASSISTANT);
        verify(episodicMemoryService, times(2)).record(any(EpisodeEvent.class));
        verify(implicitMemoryWritePipeline).persistSemanticExtractions(any(MemoryContextRequest.class), eq("hello"), eq("user"));
        verify(implicitMemoryWritePipeline).promoteRules(any(MemoryContextRequest.class), eq("hello"), eq("user"));
        verify(implicitMemoryWritePipeline).persistSemanticExtractions(any(MemoryContextRequest.class), eq("assistant"), eq("assistant"));
        verify(implicitMemoryWritePipeline).promoteRules(any(MemoryContextRequest.class), eq("assistant"), eq("assistant"));
        verifyNoInteractions(semanticMemoryService);
    }

    @Test
    void markUsedOnlyMarksProvidedMemories() {
        SemanticMemoryService semanticMemoryService = mock(SemanticMemoryService.class);
        DefaultMemoryFacade facade = facade(
                mock(MemoryRetrievalFacade.class),
                mock(ContextAssembler.class),
                mock(ConversationMemoryService.class),
                mock(EpisodicMemoryService.class),
                semanticMemoryService,
                mock(ImplicitMemoryWritePipeline.class),
                mock(EpisodicEventFactory.class),
                new MemoryIntegrationProperties()
        );

        facade.markContextMemoriesUsed(java.util.Arrays.asList(1L, null, 2L));

        verify(semanticMemoryService).markUsed(1L);
        verify(semanticMemoryService).markUsed(2L);
        verifyNoMoreInteractions(semanticMemoryService);
    }

    @Test
    void onToolExecutionWritesEpisode() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ContextAssembler assembler = mock(ContextAssembler.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        SemanticMemoryService semanticMemoryService = mock(SemanticMemoryService.class);
        ImplicitMemoryWritePipeline implicitMemoryWritePipeline = mock(ImplicitMemoryWritePipeline.class);
        EpisodicEventFactory eventFactory = mock(EpisodicEventFactory.class);
        MemoryIntegrationProperties properties = new MemoryIntegrationProperties();

        when(eventFactory.toolExecutionEvent(any(), any())).thenReturn(new EpisodeEvent());

        DefaultMemoryFacade facade = facade(
                retrievalFacade,
                assembler,
                conversationMemoryService,
                episodicMemoryService,
                semanticMemoryService,
                implicitMemoryWritePipeline,
                eventFactory,
                properties
        );

        facade.onToolExecution(request("hello"), new ToolExecutionRecord("read_file", true, "ok"));

        verify(episodicMemoryService).record(any(EpisodeEvent.class));
        verify(implicitMemoryWritePipeline).persistSemanticExtractions(any(MemoryContextRequest.class), eq("ok"), eq("tool"));
        verify(implicitMemoryWritePipeline).promoteRules(any(MemoryContextRequest.class), eq("ok"), eq("tool"));
    }

    @Test
    void disabledImplicitWritersDoNotCallPipeline() {
        ImplicitMemoryWritePipeline implicitMemoryWritePipeline = mock(ImplicitMemoryWritePipeline.class);
        EpisodicEventFactory eventFactory = mock(EpisodicEventFactory.class);
        MemoryIntegrationProperties properties = new MemoryIntegrationProperties();
        properties.setEnableSemanticExtraction(false);
        properties.setEnableRulePromotion(false);

        when(eventFactory.userMessageReceived(any())).thenReturn(new EpisodeEvent());

        DefaultMemoryFacade facade = facade(
                mock(MemoryRetrievalFacade.class),
                mock(ContextAssembler.class),
                mock(ConversationMemoryService.class),
                mock(EpisodicMemoryService.class),
                mock(SemanticMemoryService.class),
                implicitMemoryWritePipeline,
                eventFactory,
                properties
        );

        facade.onUserMessage(request("hello"));

        verifyNoInteractions(implicitMemoryWritePipeline);
    }

    private MemoryContextRequest request(String message) {
        return new MemoryContextRequest("agent-1", "user-1", "bot-1", "project-1", message, "conv-1", null, null, null, null);
    }

    private DefaultMemoryFacade facade(
            MemoryRetrievalFacade retrievalFacade,
            ContextAssembler assembler,
            ConversationMemoryService conversationMemoryService,
            EpisodicMemoryService episodicMemoryService,
            SemanticMemoryService semanticMemoryService,
            ImplicitMemoryWritePipeline implicitMemoryWritePipeline,
            EpisodicEventFactory eventFactory,
            MemoryIntegrationProperties properties
    ) {
        return new DefaultMemoryFacade(
                retrievalFacade,
                assembler,
                conversationMemoryService,
                episodicMemoryService,
                semanticMemoryService,
                implicitMemoryWritePipeline,
                eventFactory,
                properties
        );
    }
}
