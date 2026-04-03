package fr.ses10doigts.toolkitbridge.memory.retrieval.service;

import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.scoring.service.MemoryScoringService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultMemoryRetrieverTest {

    @Test
    void filtersByScopeTypeAndStatusAndSortsByScore() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, scoringService, new MemoryScopePolicy());

        MemoryEntry matchLow = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry matchHigh = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry wrongScope = entry(MemoryScope.PROJECT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry wrongType = entry(MemoryScope.AGENT, MemoryType.DECISION, MemoryStatus.ACTIVE);
        MemoryEntry archived = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ARCHIVED);

        when(repository.searchCandidates("agent-1", MemoryStatus.ACTIVE, null))
                .thenReturn(List.of(matchLow, matchHigh, wrongScope, wrongType, archived));

        when(scoringService.computeScore(matchLow)).thenReturn(1.0);
        when(scoringService.computeScore(matchHigh)).thenReturn(5.0);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                null,
                Set.of(MemoryScope.AGENT),
                Set.of(MemoryType.FACT),
                10
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).containsExactly(matchHigh, matchLow);
    }

    @Test
    void limitsResults() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, scoringService, new MemoryScopePolicy());

        MemoryEntry first = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry second = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);

        when(repository.searchCandidates("agent-1", MemoryStatus.ACTIVE, "alpha"))
                .thenReturn(List.of(first, second));

        when(scoringService.computeScore(first)).thenReturn(2.0);
        when(scoringService.computeScore(second)).thenReturn(1.0);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                "alpha",
                Set.of(),
                Set.of(),
                1
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).containsExactly(first);
    }

    @Test
    void rejectNullQuery() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, scoringService, new MemoryScopePolicy());

        assertThatThrownBy(() -> retriever.retrieve(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @Test
    void filtersProjectScopeAccordingToProjectId() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, scoringService, new MemoryScopePolicy());

        MemoryEntry projectEntry = entry(MemoryScope.PROJECT, MemoryType.FACT, MemoryStatus.ACTIVE, "project-1");
        MemoryEntry agentEntry = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);

        List<MemoryEntry> allEntries = List.of(projectEntry, agentEntry);

        when(repository.searchCandidates("agent-1", MemoryStatus.ACTIVE, null))
                .thenReturn(allEntries);

        when(scoringService.computeScore(projectEntry)).thenReturn(2.0);
        when(scoringService.computeScore(agentEntry)).thenReturn(1.0);

        MemoryQuery withProject = new MemoryQuery(
                "agent-1",
                "project-1",
                null,
                Set.of(MemoryScope.PROJECT),
                Set.of(MemoryType.FACT),
                10
        );
        List<MemoryEntry> resultWithProject = retriever.retrieve(withProject);
        assertThat(resultWithProject).containsExactly(projectEntry);

        MemoryQuery withoutProject = new MemoryQuery(
                "agent-1",
                null,
                null,
                Set.of(MemoryScope.AGENT, MemoryScope.PROJECT),
                Set.of(MemoryType.FACT),
                10
        );
        List<MemoryEntry> resultWithoutProject = retriever.retrieve(withoutProject);
        assertThat(resultWithoutProject).containsExactly(agentEntry);
    }

    @Test
    void filtersUserScopeAccordingToUserId() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        MemoryScoringService scoringService = mock(MemoryScoringService.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, scoringService, new MemoryScopePolicy());

        MemoryEntry userMatch = entry(MemoryScope.USER, MemoryType.FACT, MemoryStatus.ACTIVE, "user-1");
        MemoryEntry userOther = entry(MemoryScope.USER, MemoryType.FACT, MemoryStatus.ACTIVE, "user-2");

        when(repository.searchCandidates("agent-1", MemoryStatus.ACTIVE, null))
                .thenReturn(List.of(userMatch, userOther));

        when(scoringService.computeScore(userMatch)).thenReturn(2.0);
        when(scoringService.computeScore(userOther)).thenReturn(1.0);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                "user-1",
                null,
                null,
                Set.of(MemoryScope.USER),
                Set.of(MemoryType.FACT),
                10
        );

        List<MemoryEntry> result = retriever.retrieve(query);
        assertThat(result).containsExactly(userMatch);
    }

    private MemoryEntry entry(MemoryScope scope, MemoryType type, MemoryStatus status) {
        return entry(scope, type, status, null);
    }

    private MemoryEntry entry(MemoryScope scope, MemoryType type, MemoryStatus status, String scopeId) {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId("agent-1");
        entry.setScope(scope);
        entry.setType(type);
        entry.setContent("content");
        entry.setStatus(status);
        entry.setScopeId(scopeId);
        return entry;
    }
}
