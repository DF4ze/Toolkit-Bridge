package fr.ses10doigts.toolkitbridge.service.agent.artifact.port;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArtifactMetadataStore {

    Artifact save(Artifact artifact);

    Optional<Artifact> findByArtifactId(String artifactId);

    List<Artifact> findByTaskId(String taskId);

    List<Artifact> findExpired(Instant now);
}
