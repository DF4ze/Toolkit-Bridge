package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
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
        name = "critical_agent_trace",
        indexes = {
                @Index(name = "idx_critical_trace_occurred_at", columnList = "occurred_at"),
                @Index(name = "idx_critical_trace_agent_occurred_at", columnList = "agent_id,occurred_at"),
                @Index(name = "idx_critical_trace_type_occurred_at", columnList = "event_type,occurred_at"),
                @Index(name = "idx_critical_trace_run_id", columnList = "run_id"),
                @Index(name = "idx_critical_trace_task_id", columnList = "task_id")
        }
)
@Data
public class CriticalAgentTraceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AgentTraceEventType eventType;

    @Column(name = "source", nullable = false, length = 120)
    private String source;

    @Column(name = "run_id", length = 200)
    private String runId;

    @Column(name = "agent_id", length = 120)
    private String agentId;

    @Column(name = "message_id", length = 200)
    private String messageId;

    @Column(name = "task_id", length = 200)
    private String taskId;

    @Column(name = "attributes_json", nullable = false, columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;
}
