package fr.ses10doigts.toolkitbridge.memory.episodic.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(
        name = "episodic_memory",
        indexes = {
                @Index(name = "idx_episodic_memory_agent", columnList = "agent_id"),
                @Index(name = "idx_episodic_memory_scope", columnList = "scope"),
                @Index(name = "idx_episodic_memory_created", columnList = "created_at")
        }
)
public class EpisodeEvent implements DurableObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EpisodeScope scope;

    @Column(name = "scope_id", length = 100)
    private String scopeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EpisodeEventType type;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String action;

    @Size(max = 5000)
    @Column(length = 5000)
    private String details;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EpisodeStatus status = EpisodeStatus.UNKNOWN;

    @Column(nullable = false)
    private double score = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = EpisodeStatus.UNKNOWN;
        }
    }

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.MEMORY;
    }

    @Override
    public String persistenceDomain() {
        return "episodic";
    }
}
