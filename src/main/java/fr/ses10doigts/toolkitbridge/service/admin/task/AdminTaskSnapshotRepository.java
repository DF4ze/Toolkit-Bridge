package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AdminTaskSnapshotRepository extends JpaRepository<AdminTaskSnapshotEntity, Long> {

    Optional<AdminTaskSnapshotEntity> findByTaskId(String taskId);

    List<AdminTaskSnapshotEntity> findByOrderByLastSeenAtDescTaskIdDesc(Pageable pageable);

    List<AdminTaskSnapshotEntity> findByAssignedAgentIdOrderByLastSeenAtDescTaskIdDesc(String assignedAgentId,
                                                                                        Pageable pageable);

    List<AdminTaskSnapshotEntity> findByStatusOrderByLastSeenAtDescTaskIdDesc(TaskStatus status, Pageable pageable);

    List<AdminTaskSnapshotEntity> findByAssignedAgentIdAndStatusOrderByLastSeenAtDescTaskIdDesc(String assignedAgentId,
                                                                                                  TaskStatus status,
                                                                                                  Pageable pageable);

    @Modifying
    @Query("""
            update AdminTaskSnapshotEntity e
            set e.parentTaskId = :parentTaskId,
                e.objective = :objective,
                e.initiator = :initiator,
                e.assignedAgentId = :assignedAgentId,
                e.traceId = :traceId,
                e.entryPoint = :entryPoint,
                e.status = :status,
                e.channelType = :channelType,
                e.conversationId = :conversationId,
                e.lastSeenAt = :lastSeenAt,
                e.errorMessage = :errorMessage,
                e.artifactCount = :artifactCount
            where e.taskId = :taskId
            """)
    int updateSnapshotByTaskId(@Param("taskId") String taskId,
                               @Param("parentTaskId") String parentTaskId,
                               @Param("objective") String objective,
                               @Param("initiator") String initiator,
                               @Param("assignedAgentId") String assignedAgentId,
                               @Param("traceId") String traceId,
                               @Param("entryPoint") TaskEntryPoint entryPoint,
                               @Param("status") TaskStatus status,
                               @Param("channelType") String channelType,
                               @Param("conversationId") String conversationId,
                               @Param("lastSeenAt") Instant lastSeenAt,
                               @Param("errorMessage") String errorMessage,
                               @Param("artifactCount") int artifactCount);

    long deleteByLastSeenAtBefore(Instant cutoff);
}
