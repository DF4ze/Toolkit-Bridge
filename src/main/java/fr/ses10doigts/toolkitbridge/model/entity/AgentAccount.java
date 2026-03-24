package fr.ses10doigts.toolkitbridge.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "agent_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agent_account_bot_ident", columnNames = "agent_ident"),
                @UniqueConstraint(name = "uk_agent_account_api_key_prefix", columnNames = "api_key_prefix")
        }
)
public class AgentAccount {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agent_ident", nullable = false, length = 100)
    private String AgentIdent;

    @Column(name = "api_key_prefix", nullable = false, length = 32)
    private String apiKeyPrefix;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}