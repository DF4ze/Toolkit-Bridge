package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface HumanInterventionRepository extends JpaRepository<HumanInterventionEntity, Long> {

    Optional<HumanInterventionEntity> findByRequestId(String requestId);

    List<HumanInterventionEntity> findByStatusOrderByCreatedAtAsc(HumanInterventionStatus status);

    @Modifying
    @Query("""
            update HumanInterventionEntity e
            set e.status = :decisionStatus,
                e.decisionStatus = :decisionStatus,
                e.decisionDecidedAt = :decidedAt,
                e.decisionActorId = :actorId,
                e.decisionChannel = :channel,
                e.decisionComment = :comment,
                e.decisionMetadataJson = :decisionMetadataJson
            where e.requestId = :requestId
              and e.status = :pendingStatus
            """)
    int updateDecisionIfPending(@Param("requestId") String requestId,
                                @Param("pendingStatus") HumanInterventionStatus pendingStatus,
                                @Param("decisionStatus") HumanInterventionStatus decisionStatus,
                                @Param("decidedAt") Instant decidedAt,
                                @Param("actorId") String actorId,
                                @Param("channel") String channel,
                                @Param("comment") String comment,
                                @Param("decisionMetadataJson") String decisionMetadataJson);
}
