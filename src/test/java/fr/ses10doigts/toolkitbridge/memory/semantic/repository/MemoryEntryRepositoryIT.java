package fr.ses10doigts.toolkitbridge.memory.semantic.repository;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemoryEntryRepositoryIT {

    @Autowired
    private MemoryEntryRepository repository;

    @Test
    void findByScope() {
        MemoryEntry first = createEntry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "content one");
        MemoryEntry second = createEntry("agent-1", MemoryScope.PROJECT, MemoryType.FACT, "content two");
        repository.save(first);
        repository.save(second);

        List<MemoryEntry> result = repository.findByAgentIdAndScope("agent-1", MemoryScope.AGENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScope()).isEqualTo(MemoryScope.AGENT);
    }

    @Test
    void findByType() {
        repository.save(createEntry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "content one"));
        repository.save(createEntry("agent-1", MemoryScope.AGENT, MemoryType.DECISION, "content two"));

        List<MemoryEntry> result = repository.findByAgentIdAndType("agent-1", MemoryType.DECISION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(MemoryType.DECISION);
    }

    @Test
    void searchByContent() {
        repository.save(createEntry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "alpha beta"));
        repository.save(createEntry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "gamma"));

        List<MemoryEntry> result = repository.searchByAgentIdAndContent("agent-1", "beta");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).contains("beta");
    }

    @Test
    void isolationByAgent() {
        repository.save(createEntry("agent-1", MemoryScope.AGENT, MemoryType.FACT, "shared data"));
        repository.save(createEntry("agent-2", MemoryScope.AGENT, MemoryType.FACT, "shared data"));

        List<MemoryEntry> result = repository.searchByAgentIdAndContent("agent-1", "shared");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgentId()).isEqualTo("agent-1");
    }

    private MemoryEntry createEntry(String agentId, MemoryScope scope, MemoryType type, String content) {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId(agentId);
        entry.setScope(scope);
        entry.setType(type);
        entry.setContent(content);
        entry.setImportance(0.5);
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setTags(Set.of("tag"));
        return entry;
    }
}
