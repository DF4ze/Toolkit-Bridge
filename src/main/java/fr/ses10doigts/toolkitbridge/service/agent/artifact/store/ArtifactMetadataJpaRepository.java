package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArtifactMetadataJpaRepository extends JpaRepository<ArtifactMetadataEntity, Long> {

    Optional<ArtifactMetadataEntity> findByArtifactId(String artifactId);

    List<ArtifactMetadataEntity> findByTaskId(String taskId);

    List<ArtifactMetadataEntity> findByExpiresAtBefore(Instant now);
}
