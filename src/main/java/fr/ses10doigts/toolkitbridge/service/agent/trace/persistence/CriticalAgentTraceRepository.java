package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CriticalAgentTraceRepository extends JpaRepository<CriticalAgentTraceEntity, Long> {

    List<CriticalAgentTraceEntity> findByOrderByOccurredAtDescIdDesc(Pageable pageable);

    List<CriticalAgentTraceEntity> findByAgentIdOrderByOccurredAtDescIdDesc(String agentId, Pageable pageable);

    long deleteByOccurredAtBefore(Instant cutoff);
}
