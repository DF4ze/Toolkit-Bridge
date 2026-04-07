package fr.ses10doigts.toolkitbridge.memory.rule.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import jakarta.persistence.Column;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(
        name = "rule_memory",
        indexes = {
                @Index(name = "idx_rule_memory_scope", columnList = "scope"),
                @Index(name = "idx_rule_memory_agent", columnList = "agent_id"),
                @Index(name = "idx_rule_memory_scope_id", columnList = "scope_id"),
                @Index(name = "idx_rule_memory_status", columnList = "status")
        }
)
public class RuleEntry implements DurableObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleScope scope;

    @Column(name = "scope_id", length = 100)
    private String scopeId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RulePriority priority = RulePriority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleStatus status = RuleStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "write_mode", length = 20)
    private MemoryWriteMode writeMode = MemoryWriteMode.EXPLICIT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
            status = RuleStatus.ACTIVE;
        }
        if (priority == null) {
            priority = RulePriority.MEDIUM;
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
        return "rule";
    }
}
