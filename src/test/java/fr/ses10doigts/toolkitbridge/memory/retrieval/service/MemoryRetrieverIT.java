package fr.ses10doigts.toolkitbridge.memory.retrieval.service;

import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "toolkit.memory.retrieval.max-candidate-pool-size=5")
@Transactional
@Import(MemoryRetrieverIT.FixedClockConfig.class)
class MemoryRetrieverIT {

    private static final Instant NOW = Instant.parse("2026-03-26T00:00:00Z");

    @Autowired
    private MemoryEntryRepository repository;

    @Autowired
    private MemoryRetriever retriever;

    @Test
    void retrieveFiltersAndRespectsCandidatePoolBound() {
        MemoryEntry recent = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha recent");
        recent.setImportance(0.1);
        recent.setLastAccessedAt(NOW.minusSeconds(86400L));
        repository.save(recent);

        MemoryEntry older = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha older");
        older.setImportance(0.9);
        older.setLastAccessedAt(NOW.minusSeconds(86400L * 10));
        repository.save(older);

        MemoryEntry archived = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha archived");
        archived.setStatus(MemoryStatus.ARCHIVED);
        repository.save(archived);

        MemoryEntry wrongScope = entry("agent-1", MemoryScope.PROJECT, MemoryType.FACT, "alpha project");
        repository.save(wrongScope);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                null,
                "alpha",
                Set.of(MemoryScope.AGENT),
                Set.of(MemoryType.FACT),
                5
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).containsExactly(older, recent);
    }

    @Test
    void matchesTagsWhenTextQueryProvided() {
        MemoryEntry tagged = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "no match here");
        tagged.setTags(Set.of("tag-alpha"));
        repository.save(tagged);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                null,
                "tag-alpha",
                Set.of(),
                Set.of(),
                5
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).contains(tagged);
    }

    private MemoryEntry entry(String agentId, MemoryScope scope, MemoryType type, String content) {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId(agentId);
        entry.setScope(scope);
        entry.setType(type);
        entry.setContent(content);
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setImportance(0.5);
        return entry;
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean("testMemoryScoringClock")
        @Primary
        public Clock memoryScoringClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
