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

@SpringBootTest
@Transactional
@Import(MemoryRetrieverIT.FixedClockConfig.class)
class MemoryRetrieverIT {

    private static final Instant NOW = Instant.parse("2026-03-26T00:00:00Z");

    @Autowired
    private MemoryEntryRepository repository;

    @Autowired
    private MemoryRetriever retriever;

    @Test
    void retrieveFiltersAndSortsAndRespectsLimit() {
        MemoryEntry high = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha content");
        high.setImportance(0.9);
        high.setLastAccessedAt(NOW.minusSeconds(86400L * 10));
        high.setTags(Set.of("alpha"));
        repository.save(high);

        MemoryEntry low = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "beta content");
        low.setImportance(0.2);
        low.setLastAccessedAt(NOW.minusSeconds(86400L * 1));
        repository.save(low);

        MemoryEntry archived = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha archived");
        archived.setStatus(MemoryStatus.ARCHIVED);
        repository.save(archived);

        MemoryEntry wrongScope = entry("agent-1", MemoryScope.PROJECT, MemoryType.FACT, "alpha project");
        repository.save(wrongScope);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                "alpha",
                Set.of(MemoryScope.AGENT),
                Set.of(MemoryType.FACT),
                1
        );

        List<MemoryEntry> result = retriever.retrieve(query);

        assertThat(result).containsExactly(high);
    }

    @Test
    void matchesTagsWhenTextQueryProvided() {
        MemoryEntry tagged = entry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "no match here");
        tagged.setTags(Set.of("tag-alpha"));
        repository.save(tagged);

        MemoryQuery query = new MemoryQuery(
                "agent-1",
                null,
                "tag-alpha",
                Set.of(),
                Set.of(),
                10
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

        @Bean
        @Primary
        public Clock memoryScoringClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
