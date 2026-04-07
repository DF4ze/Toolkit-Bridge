package fr.ses10doigts.toolkitbridge.memory.semantic.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(
        name = "semantic_memory",
        indexes = {
                @Index(name = "idx_semantic_memory_agent", columnList = "agent_id"),
                @Index(name = "idx_semantic_memory_scope", columnList = "scope"),
                @Index(name = "idx_semantic_memory_type", columnList = "type")
        }
)
public class MemoryEntry implements ScorableMemory, DurableObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryScope scope;

    @Column(name = "scope_id", length = 100)
    private String scopeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryType type;

    @NotBlank
    @Size(max = 5000)
    @Column(length = 5000, nullable = false)
    private String content;

    @DecimalMin("0.0")
    @Column(nullable = false)
    private double importance = 0.0;

    @Column(nullable = false)
    private int usageCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryStatus status = MemoryStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "write_mode", length = 20)
    private MemoryWriteMode writeMode = MemoryWriteMode.EXPLICIT;

    @ElementCollection
    @CollectionTable(name = "semantic_memory_tags", joinColumns = @JoinColumn(name = "memory_id"))
    @Column(name = "tag", length = 100)
    private Set<String> tags = new HashSet<>();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = MemoryStatus.ACTIVE;
        }
        if (writeMode == null) {
            writeMode = MemoryWriteMode.EXPLICIT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.MEMORY;
    }

    @Override
    public String persistenceDomain() {
        return type == null ? "semantic" : "semantic_" + type.name().toLowerCase(java.util.Locale.ROOT);
    }
}
