package fr.ses10doigts.toolkitbridge.memory.episodic.repository;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db"
})
@Transactional
class EpisodeEventRepositoryIT {

    @Autowired
    private EpisodeEventRepository repository;

    @Test
    void findRecentByAgentOrdersDescAndLimits() {
        repository.save(event("agent-1", EpisodeScope.AGENT, Instant.parse("2026-03-26T00:00:00Z")));
        repository.save(event("agent-1", EpisodeScope.AGENT, Instant.parse("2026-03-27T00:00:00Z")));
        repository.save(event("agent-1", EpisodeScope.AGENT, Instant.parse("2026-03-25T00:00:00Z")));

        List<EpisodeEvent> result = repository.findByAgentIdOrderByCreatedAtDesc("agent-1", PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt()).isAfter(result.get(1).getCreatedAt());
    }

    @Test
    void findRecentByScopeFilters() {
        repository.save(event("agent-1", EpisodeScope.AGENT, Instant.parse("2026-03-26T00:00:00Z")));
        repository.save(event("agent-1", EpisodeScope.PROJECT, Instant.parse("2026-03-27T00:00:00Z")));

        List<EpisodeEvent> result = repository.findByAgentIdAndScopeOrderByCreatedAtDesc(
                "agent-1",
                EpisodeScope.PROJECT,
                PageRequest.of(0, 10)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScope()).isEqualTo(EpisodeScope.PROJECT);
    }

    @Test
    void isolationByAgent() {
        repository.save(event("agent-1", EpisodeScope.AGENT, Instant.parse("2026-03-26T00:00:00Z")));
        repository.save(event("agent-2", EpisodeScope.AGENT, Instant.parse("2026-03-27T00:00:00Z")));

        List<EpisodeEvent> result = repository.findByAgentIdOrderByCreatedAtDesc("agent-1", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgentId()).isEqualTo("agent-1");
    }

    private EpisodeEvent event(String agentId, EpisodeScope scope, Instant createdAt) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(agentId);
        event.setScope(scope);
        event.setType(EpisodeEventType.ACTION);
        event.setAction("ACTION");
        event.setStatus(EpisodeStatus.SUCCESS);
        event.setCreatedAt(createdAt);
        event.setScore(0.0);
        return event;
    }
}
