package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(
        name = "admin_task_snapshot",
        indexes = {
                @Index(name = "idx_admin_task_task_id", columnList = "task_id", unique = true),
                @Index(name = "idx_admin_task_last_seen", columnList = "last_seen_at"),
                @Index(name = "idx_admin_task_agent_last_seen", columnList = "assigned_agent_id,last_seen_at"),
                @Index(name = "idx_admin_task_status_last_seen", columnList = "status,last_seen_at"),
                @Index(name = "idx_admin_task_trace_id", columnList = "trace_id")
        }
)
@Data
public class AdminTaskSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 100)
    private String taskId;

    @Column(name = "parent_task_id", length = 100)
    private String parentTaskId;

    @Column(name = "objective", nullable = false, length = 2000)
    private String objective;

    @Column(name = "initiator", nullable = false, length = 200)
    private String initiator;

    @Column(name = "assigned_agent_id", nullable = false, length = 100)
    private String assignedAgentId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_point", nullable = false, length = 50)
    private TaskEntryPoint entryPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "channel_type", length = 100)
    private String channelType;

    @Column(name = "conversation_id", length = 200)
    private String conversationId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "artifact_count", nullable = false)
    private int artifactCount;
}
