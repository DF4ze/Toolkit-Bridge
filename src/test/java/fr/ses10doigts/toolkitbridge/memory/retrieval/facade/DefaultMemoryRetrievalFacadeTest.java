package fr.ses10doigts.toolkitbridge.memory.retrieval.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultMemoryRetrievalFacadeTest {

    @Test
    void returnsAllSectionsFromDependencies() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setMaxRules(2);
        properties.setMaxSemanticMemories(2);
        properties.setMaxEpisodes(1);
        properties.setConversationSliceMaxCharacters(25);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                episodicMemoryService,
                conversationMemoryService,
                properties
        );

        RuleEntry firstRule = rule("rule-1");
        RuleEntry secondRule = rule("rule-2");
        when(ruleService.getApplicableRules("agent-1", "project-1"))
                .thenReturn(List.of(firstRule, secondRule));

        MemoryEntry firstMemory = memoryEntry("memory-1");
        MemoryEntry secondMemory = memoryEntry("memory-2");
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(firstMemory, secondMemory));

        EpisodeEvent episode = new EpisodeEvent();
        episode.setAgentId("agent-1");
        episode.setScope(EpisodeScope.AGENT);
        episode.setType(EpisodeEventType.RESULT);
        episode.setStatus(EpisodeStatus.SUCCESS);
        episode.setAction("action");
        episode.setDetails("detail");
        episode.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        when(episodicMemoryService.findRecent("agent-1", 1)).thenReturn(List.of(episode));
        when(episodicMemoryService.findRecentByScope("agent-1", EpisodeScope.PROJECT, 5))
                .thenReturn(List.of());

        when(conversationMemoryService.buildContext(any(ConversationMemoryKey.class)))
                .thenReturn("conversation-slice");

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", "project-1", "hello");
        RetrievedMemories result = facade.retrieve(request);

        assertThat(result.rules()).containsExactly(firstRule, secondRule);
        assertThat(result.semanticMemories()).containsExactly(firstMemory, secondMemory);
        assertThat(result.episodicMemories()).hasSize(1);
        assertThat(result.episodicMemories().get(0).summary()).endsWith("detail");
        assertThat(result.conversationSlice()).isEqualTo("conversation-slice");
    }

    @Test
    void trimsConversationSliceWhenTooLong() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setConversationSliceMaxCharacters(4);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                episodicMemoryService,
                conversationMemoryService,
                properties
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(episodicMemoryService.findRecent(any(), anyInt())).thenReturn(List.of());
        when(conversationMemoryService.buildContext(any())).thenReturn("abcdef");

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", null, "message");
        RetrievedMemories result = facade.retrieve(request);

        assertThat(result.conversationSlice()).isEqualTo("cdef");
    }

    @Test
    void includesSuccessAndFailureEpisodesAndFiltersProjects() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setMaxEpisodes(3);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                episodicMemoryService,
                conversationMemoryService,
                properties
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(conversationMemoryService.buildContext(any())).thenReturn("");

        EpisodeEvent success = episode("success", EpisodeScope.AGENT, null, EpisodeStatus.SUCCESS);
        EpisodeEvent failure = episode("failure", EpisodeScope.AGENT, null, EpisodeStatus.FAILURE);
        EpisodeEvent projectMatch = episode("project", EpisodeScope.PROJECT, "project-1", EpisodeStatus.SUCCESS);
        EpisodeEvent projectMismatch = episode("other", EpisodeScope.PROJECT, "other", EpisodeStatus.SUCCESS);

        when(episodicMemoryService.findRecent("agent-1", 3)).thenReturn(List.of(success, failure));
        when(episodicMemoryService.findRecentByScope("agent-1", EpisodeScope.PROJECT, 5))
                .thenReturn(List.of(projectMatch, projectMismatch));

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", "project-1", "msg");
        RetrievedMemories result = facade.retrieve(request);

        assertThat(result.episodicMemories()).hasSize(3);
        assertThat(result.episodicMemories()).extracting(RetrievedMemories.EpisodeSummary::summary)
                .anyMatch(text -> text.contains("success"))
                .anyMatch(text -> text.contains("failure"));
        assertThat(result.episodicMemories()).extracting(RetrievedMemories.EpisodeSummary::scopeId)
                .contains("project-1")
                .doesNotContain("other");
    }

    @Test
    void ignoresOutOfScopeProjectEpisodesWhenProjectMissing() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setMaxEpisodes(2);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                episodicMemoryService,
                conversationMemoryService,
                properties
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(conversationMemoryService.buildContext(any())).thenReturn("");

        EpisodeEvent projectOnly = episode("project-only", EpisodeScope.PROJECT, "project-1", EpisodeStatus.SUCCESS);
        when(episodicMemoryService.findRecent("agent-1", 2)).thenReturn(List.of(projectOnly));
        when(episodicMemoryService.findRecentByScope("agent-1", EpisodeScope.PROJECT, 5))
                .thenReturn(List.of(projectOnly));

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", null, "msg");
        RetrievedMemories result = facade.retrieve(request);

        assertThat(result.episodicMemories()).isEmpty();
    }

    @Test
    void appliesRequestOverridesForSemanticAndEpisodeLimits() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setMaxSemanticMemories(10);
        properties.setMaxEpisodes(8);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                episodicMemoryService,
                conversationMemoryService,
                properties
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(episodicMemoryService.findRecent(any(), anyInt())).thenReturn(List.of());
        when(conversationMemoryService.buildContext(any())).thenReturn("");

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", "project-1", "msg", 3, 2);
        facade.retrieve(request);

        ArgumentCaptor<fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery> queryCaptor =
                ArgumentCaptor.forClass(fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery.class);
        verify(memoryRetriever).retrieve(queryCaptor.capture());
        assertThat(queryCaptor.getValue().limit()).isEqualTo(3);
        verify(episodicMemoryService).findRecent("agent-1", 2);
    }

    private RuleEntry rule(String name) {
        RuleEntry entry = new RuleEntry();
        entry.setScope(RuleScope.AGENT);
        entry.setPriority(RulePriority.HIGH);
        entry.setStatus(RuleStatus.ACTIVE);
        entry.setTitle(name + "-title");
        entry.setContent(name + "-content");
        return entry;
    }

    private MemoryEntry memoryEntry(String content) {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId("agent-1");
        entry.setScope(MemoryScope.AGENT);
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setType(MemoryType.FACT);
        entry.setContent(content);
        return entry;
    }

    private EpisodeEvent episode(String action, EpisodeScope scope, String scopeId, EpisodeStatus status) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId("agent-1");
        event.setScope(scope);
        event.setScopeId(scopeId);
        event.setType(EpisodeEventType.RESULT);
        event.setStatus(status);
        event.setAction(action);
        event.setDetails(action + "-details");
        return event;
    }
}
