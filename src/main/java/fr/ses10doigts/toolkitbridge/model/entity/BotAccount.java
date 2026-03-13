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
        name = "bot_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bot_account_bot_ident", columnNames = "bot_ident"),
                @UniqueConstraint(name = "uk_bot_account_api_key_prefix", columnNames = "api_key_prefix")
        }
)
public class BotAccount {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "bot_ident", nullable = false, length = 100)
    private String botIdent;

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