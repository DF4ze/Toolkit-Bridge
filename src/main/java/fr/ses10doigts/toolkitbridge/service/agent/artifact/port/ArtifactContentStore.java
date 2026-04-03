package fr.ses10doigts.toolkitbridge.service.agent.artifact.port;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;

public interface ArtifactContentStore {

    ArtifactContentPointer store(String artifactId,
                                 ArtifactType type,
                                 String content,
                                 String mediaType,
                                 String fileName);
}
