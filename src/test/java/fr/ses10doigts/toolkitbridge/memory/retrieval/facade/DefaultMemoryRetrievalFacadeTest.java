package fr.ses10doigts.toolkitbridge.memory.retrieval.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfiguration;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.scoring.service.MemoryScoringService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultMemoryRetrievalFacadeTest {

    @Test
    void returnsAllSectionsFromDependencies() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRuntimeConfigurationResolver resolver = resolver(2, 2, 4, 1, 5, 25);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                scoringService,
                episodicMemoryService,
                conversationMemoryService,
                resolver,
                new MemoryScopePolicy()
        );

        RuleEntry firstRule = rule("rule-1");
        RuleEntry secondRule = rule("rule-2");
        when(ruleService.getApplicableRules("agent-1", "project-1"))
                .thenReturn(List.of(firstRule, secondRule));

        MemoryEntry firstMemory = memoryEntry("memory-1");
        MemoryEntry secondMemory = memoryEntry("memory-2");
        when(memoryRetriever.retrieve(any())).thenReturn(List.of(firstMemory, secondMemory));
        when(scoringService.rank(List.of(firstMemory, secondMemory), 2)).thenReturn(List.of(secondMemory, firstMemory));

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
        assertThat(result.semanticMemories()).containsExactly(secondMemory, firstMemory);
        assertThat(result.episodicMemories()).hasSize(1);
        assertThat(result.episodicMemories().get(0).summary()).endsWith("detail");
        assertThat(result.conversationSlice()).isEqualTo("conversation-slice");
    }

    @Test
    void trimsConversationSliceWhenTooLong() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRuntimeConfigurationResolver resolver = resolver(10, 10, 25, 5, 5, 4);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                scoringService,
                episodicMemoryService,
                conversationMemoryService,
                resolver,
                new MemoryScopePolicy()
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(scoringService.rank(org.mockito.ArgumentMatchers.<MemoryEntry>anyList(), eq(10))).thenReturn(List.of());
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
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRuntimeConfigurationResolver resolver = resolver(10, 10, 25, 3, 5, 4000);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                scoringService,
                episodicMemoryService,
                conversationMemoryService,
                resolver,
                new MemoryScopePolicy()
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(scoringService.rank(org.mockito.ArgumentMatchers.<MemoryEntry>anyList(), eq(10))).thenReturn(List.of());
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
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRuntimeConfigurationResolver resolver = resolver(10, 10, 25, 2, 5, 4000);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                scoringService,
                episodicMemoryService,
                conversationMemoryService,
                resolver,
                new MemoryScopePolicy()
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(scoringService.rank(org.mockito.ArgumentMatchers.<MemoryEntry>anyList(), eq(10))).thenReturn(List.of());
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
    void appliesRequestOverridesForSemanticAndEpisodeLimitsAndUsesCandidatePool() {
        RuleService ruleService = mock(RuleService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        EpisodicMemoryService episodicMemoryService = mock(EpisodicMemoryService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRuntimeConfigurationResolver resolver = resolver(10, 10, 6, 8, 5, 4000);

        DefaultMemoryRetrievalFacade facade = new DefaultMemoryRetrievalFacade(
                ruleService,
                memoryRetriever,
                scoringService,
                episodicMemoryService,
                conversationMemoryService,
                resolver,
                new MemoryScopePolicy()
        );

        when(ruleService.getApplicableRules(any(), any())).thenReturn(List.of());
        when(memoryRetriever.retrieve(any())).thenReturn(List.of());
        when(scoringService.rank(org.mockito.ArgumentMatchers.<MemoryEntry>anyList(), eq(3))).thenReturn(List.of());
        when(episodicMemoryService.findRecent(any(), anyInt())).thenReturn(List.of());
        when(conversationMemoryService.buildContext(any())).thenReturn("");

        ContextRequest request = new ContextRequest("agent-1", "conversation-1", "project-1", "msg", 3, 2);
        facade.retrieve(request);

        ArgumentCaptor<MemoryQuery> queryCaptor = ArgumentCaptor.forClass(MemoryQuery.class);
        verify(memoryRetriever).retrieve(queryCaptor.capture());
        assertThat(queryCaptor.getValue().candidateLimit()).isEqualTo(6);
        assertThat(queryCaptor.getValue().scopes()).containsExactlyInAnyOrder(
                MemoryScope.SYSTEM,
                MemoryScope.AGENT,
                MemoryScope.USER,
                MemoryScope.PROJECT,
                MemoryScope.SHARED
        );
        verify(scoringService).rank(org.mockito.ArgumentMatchers.<MemoryEntry>anyList(), eq(3));
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

    private MemoryRuntimeConfigurationResolver resolver(
            int maxRules,
            int maxSemanticMemories,
            int maxCandidatePoolSize,
            int maxEpisodes,
            int maxProjectEpisodeFetch,
            int conversationSliceMaxCharacters
    ) {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        when(resolver.snapshot()).thenReturn(new MemoryRuntimeConfiguration(
                new MemoryRuntimeConfiguration.Context(10, 10, 15000, 5),
                new MemoryRuntimeConfiguration.Retrieval(
                        maxRules,
                        maxSemanticMemories,
                        maxCandidatePoolSize,
                        maxEpisodes,
                        maxProjectEpisodeFetch,
                        conversationSliceMaxCharacters
                ),
                new MemoryRuntimeConfiguration.Integration(true, true, true, true),
                new MemoryRuntimeConfiguration.Scoring(1.0, 0.5, 1.0),
                new MemoryRuntimeConfiguration.GlobalContext(
                        true,
                        MemoryRuntimeConfiguration.GlobalContextLoadMode.ON_DEMAND,
                        java.time.Duration.ofSeconds(30),
                        List.of()
                )
        ));
        return resolver;
    }
}
