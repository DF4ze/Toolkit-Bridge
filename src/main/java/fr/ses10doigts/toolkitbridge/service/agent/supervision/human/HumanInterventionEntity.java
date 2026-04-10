package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
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
        name = "human_intervention",
        indexes = {
                @Index(name = "idx_human_intervention_request_id", columnList = "request_id", unique = true),
                @Index(name = "idx_human_intervention_status_created_at", columnList = "status,created_at")
        }
)
@Data
public class HumanInterventionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitive_action", nullable = false, length = 50)
    private AgentSensitiveAction sensitiveAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private HumanInterventionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private HumanInterventionStatus status;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "detail", length = 5000)
    private String detail;

    @Column(name = "request_metadata_json", length = 4096)
    private String requestMetadataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", length = 30)
    private HumanInterventionStatus decisionStatus;

    @Column(name = "decision_decided_at")
    private Instant decisionDecidedAt;

    @Column(name = "decision_actor_id", length = 100)
    private String decisionActorId;

    @Column(name = "decision_channel", length = 100)
    private String decisionChannel;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decision_metadata_json", length = 4096)
    private String decisionMetadataJson;
}
