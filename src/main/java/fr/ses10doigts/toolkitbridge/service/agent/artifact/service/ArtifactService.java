package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactContentStore;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactMetadataStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ArtifactService {

    private final ArtifactMetadataStore metadataStore;
    private final ArtifactContentStore contentStore;
    private final ArtifactRetentionPolicy retentionPolicy;

    public ArtifactService(ArtifactMetadataStore metadataStore,
                           ArtifactContentStore contentStore,
                           ArtifactRetentionPolicy retentionPolicy) {
        this.metadataStore = metadataStore;
        this.contentStore = contentStore;
        this.retentionPolicy = retentionPolicy;
    }

    public Artifact create(ArtifactDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }
        String artifactId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        ArtifactContentPointer pointer = contentStore.store(
                artifactId,
                draft.type(),
                draft.content(),
                draft.mediaType(),
                draft.fileName()
        );

        Artifact artifact = new Artifact(
                artifactId,
                draft.type(),
                draft.taskId(),
                draft.producerAgentId(),
                draft.title(),
                createdAt,
                retentionPolicy.computeExpiration(draft.type(), createdAt),
                draft.metadata(),
                pointer
        );
        return metadataStore.save(artifact);
    }

    public Optional<Artifact> findById(String artifactId) {
        return metadataStore.findByArtifactId(artifactId);
    }

    public List<Artifact> findByTaskId(String taskId) {
        return metadataStore.findByTaskId(taskId);
    }

    public List<Artifact> findByProducerAgentId(String producerAgentId, int limit) {
        if (producerAgentId == null || producerAgentId.isBlank()) {
            return List.of();
        }
        return metadataStore.findByProducerAgentId(producerAgentId, limit);
    }

    public List<Artifact> findRecent(int limit) {
        return metadataStore.findRecent(limit);
    }

    public List<Artifact> findExpired(Instant now) {
        return metadataStore.findExpired(now == null ? Instant.now() : now);
    }
}
