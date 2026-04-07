package fr.ses10doigts.toolkitbridge.memory.retrieval.service;

import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultMemoryRetrieverTest {

    @Test
    void filtersByScopeTypeAndVisibilityWithoutScoring() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, properties(10), new MemoryScopePolicy());

        MemoryEntry match = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry wrongScope = entry(MemoryScope.PROJECT, MemoryType.FACT, MemoryStatus.ACTIVE);
        MemoryEntry wrongType = entry(MemoryScope.AGENT, MemoryType.DECISION, MemoryStatus.ACTIVE);
        MemoryEntry archived = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ARCHIVED);

        when(repository.searchCandidates(eq("agent-1"), eq(MemoryStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(List.of(match, wrongScope, wrongType, archived));

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                null,
                null,
                Set.of(MemoryScope.AGENT),
                Set.of(MemoryType.FACT),
                10
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).containsExactly(match);
    }

    @Test
    void usesConfiguredCandidatePoolBound() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, properties(2), new MemoryScopePolicy());

        when(repository.searchCandidates(eq("agent-1"), eq(MemoryStatus.ACTIVE), eq("alpha"), any(Pageable.class)))
                .thenReturn(List.of());

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                null,
                "alpha",
                Set.of(),
                Set.of(),
                5
        );

        retriever.retrieve(query);

        verify(repository).searchCandidates(
                org.mockito.Mockito.eq("agent-1"),
                org.mockito.Mockito.eq(MemoryStatus.ACTIVE),
                org.mockito.Mockito.eq("alpha"),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 2)
        );
    }

    @Test
    void rejectsNullQuery() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, properties(10), new MemoryScopePolicy());

        assertThatThrownBy(() -> retriever.retrieve(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
    }

    @Test
    void filtersProjectScopeAccordingToProjectId() {
        MemoryEntryRepository repository = mock(MemoryEntryRepository.class);
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, properties(10), new MemoryScopePolicy());

        MemoryEntry projectEntry = entry(MemoryScope.PROJECT, MemoryType.FACT, MemoryStatus.ACTIVE, "project-1");
        MemoryEntry agentEntry = entry(MemoryScope.AGENT, MemoryType.FACT, MemoryStatus.ACTIVE);

        when(repository.searchCandidates(eq("agent-1"), eq(MemoryStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(List.of(projectEntry, agentEntry));

        MemoryQuery withProject = new MemoryQuery(
                "agent-1",
                null,
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
        DefaultMemoryRetriever retriever = new DefaultMemoryRetriever(repository, properties(10), new MemoryScopePolicy());

        MemoryEntry userMatch = entry(MemoryScope.USER, MemoryType.FACT, MemoryStatus.ACTIVE, "user-1");
        MemoryEntry userOther = entry(MemoryScope.USER, MemoryType.FACT, MemoryStatus.ACTIVE, "user-2");

        when(repository.searchCandidates(eq("agent-1"), eq(MemoryStatus.ACTIVE), isNull(), any(Pageable.class)))
                .thenReturn(List.of(userMatch, userOther));

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

    private MemoryRetrievalProperties properties(int maxCandidatePoolSize) {
        MemoryRetrievalProperties properties = new MemoryRetrievalProperties();
        properties.setMaxCandidatePoolSize(maxCandidatePoolSize);
        return properties;
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
