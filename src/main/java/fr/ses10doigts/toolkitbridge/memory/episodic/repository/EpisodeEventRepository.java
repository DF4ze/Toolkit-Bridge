package fr.ses10doigts.toolkitbridge.memory.episodic.repository;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeEventRepository extends JpaRepository<EpisodeEvent, Long> {

    List<EpisodeEvent> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    List<EpisodeEvent> findByAgentIdAndScopeOrderByCreatedAtDesc(String agentId, EpisodeScope scope, Pageable pageable);
}
