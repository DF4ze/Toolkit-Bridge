package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@Table(
        name = "artifact_metadata",
        indexes = {
                @Index(name = "idx_artifact_metadata_artifact_id", columnList = "artifact_id", unique = true),
                @Index(name = "idx_artifact_metadata_task_id", columnList = "task_id"),
                @Index(name = "idx_artifact_metadata_type", columnList = "type"),
                @Index(name = "idx_artifact_metadata_expires_at", columnList = "expires_at")
        }
)
public class ArtifactMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false, length = 100, unique = true)
    private String artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ArtifactType type;

    @Column(name = "task_id", nullable = false, length = 100)
    private String taskId;

    @Column(name = "producer_agent_id", nullable = false, length = 100)
    private String producerAgentId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "content_storage_kind", nullable = false, length = 50)
    private String contentStorageKind;

    @Column(name = "content_location", nullable = false, length = 500)
    private String contentLocation;

    @Column(name = "content_media_type", nullable = false, length = 120)
    private String contentMediaType;

    @Column(name = "content_size_bytes", nullable = false)
    private long contentSizeBytes;

    @ElementCollection
    @CollectionTable(name = "artifact_metadata_attributes", joinColumns = @JoinColumn(name = "artifact_metadata_id"))
    @MapKeyColumn(name = "attribute_key", length = 120)
    @Column(name = "attribute_value", length = 2000)
    private Map<String, String> metadata = new HashMap<>();
}
