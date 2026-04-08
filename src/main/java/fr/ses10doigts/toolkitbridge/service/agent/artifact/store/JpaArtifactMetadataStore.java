package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactMetadataStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JpaArtifactMetadataStore implements ArtifactMetadataStore {

    private final ArtifactMetadataJpaRepository repository;

    public JpaArtifactMetadataStore(ArtifactMetadataJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Artifact save(Artifact artifact) {
        ArtifactMetadataEntity entity = toEntity(artifact);
        ArtifactMetadataEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Artifact> findByArtifactId(String artifactId) {
        return repository.findByArtifactId(artifactId).map(this::toDomain);
    }

    @Override
    public List<Artifact> findByTaskId(String taskId) {
        return repository.findByTaskId(taskId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Artifact> findByProducerAgentId(String producerAgentId, int limit) {
        int effectiveLimit = Math.max(limit, 1);
        return repository.findByProducerAgentIdOrderByCreatedAtDesc(producerAgentId, PageRequest.of(0, effectiveLimit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Artifact> findRecent(int limit) {
        int effectiveLimit = Math.max(limit, 1);
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, effectiveLimit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Artifact> findExpired(Instant now) {
        return repository.findByExpiresAtBefore(now).stream().map(this::toDomain).toList();
    }

    private ArtifactMetadataEntity toEntity(Artifact artifact) {
        ArtifactMetadataEntity entity = repository.findByArtifactId(artifact.artifactId())
                .orElseGet(ArtifactMetadataEntity::new);

        entity.setArtifactId(artifact.artifactId());
        entity.setType(artifact.type());
        entity.setTaskId(artifact.taskId());
        entity.setProducerAgentId(artifact.producerAgentId());
        entity.setTitle(artifact.title());
        entity.setCreatedAt(artifact.createdAt());
        entity.setExpiresAt(artifact.expiresAt());
        entity.setContentStorageKind(artifact.contentPointer().storageKind());
        entity.setContentLocation(artifact.contentPointer().location());
        entity.setContentMediaType(artifact.contentPointer().mediaType());
        entity.setContentSizeBytes(artifact.contentPointer().sizeBytes());
        entity.setMetadata(artifact.metadata() == null ? new HashMap<>() : new HashMap<>(artifact.metadata()));
        return entity;
    }

    private Artifact toDomain(ArtifactMetadataEntity entity) {
        return new Artifact(
                entity.getArtifactId(),
                entity.getType(),
                entity.getTaskId(),
                entity.getProducerAgentId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getMetadata(),
                new ArtifactContentPointer(
                        entity.getContentStorageKind(),
                        entity.getContentLocation(),
                        entity.getContentMediaType(),
                        entity.getContentSizeBytes()
                )
        );
    }
}
