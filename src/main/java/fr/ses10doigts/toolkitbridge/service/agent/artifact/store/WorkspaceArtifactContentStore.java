package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactContentStore;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Repository
public class WorkspaceArtifactContentStore implements ArtifactContentStore {

    private static final String WORKSPACE_STORAGE_KIND = "workspace";

    private final WorkspaceService workspaceService;
    private final ArtifactStorageProperties properties;

    public WorkspaceArtifactContentStore(WorkspaceService workspaceService,
                                         ArtifactStorageProperties properties) {
        this.workspaceService = workspaceService;
        this.properties = properties;
    }

    @Override
    public ArtifactContentPointer store(String artifactId,
                                        ArtifactType type,
                                        String content,
                                        String mediaType,
                                        String fileName) {
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        String safeFileName = sanitizeFileName(fileName);
        String safeMediaType = (mediaType == null || mediaType.isBlank()) ? "text/plain" : mediaType.trim();
        String safeContent = content == null ? "" : content;
        byte[] contentBytes = safeContent.getBytes(StandardCharsets.UTF_8);

        try {
            Path sharedRoot = workspaceService.getSharedWorkspace();
            Path storageRoot = sharedRoot.resolve(properties.getContentFolder()).normalize();
            if (!storageRoot.startsWith(sharedRoot)) {
                throw new IllegalArgumentException("Configured artifact content-folder escapes shared workspace");
            }
            Path typeDirectory = storageRoot.resolve(type.key()).normalize();
            Path artifactDirectory = typeDirectory.resolve(artifactId).normalize();
            Files.createDirectories(artifactDirectory);

            Path artifactFile = artifactDirectory.resolve(safeFileName).normalize();
            if (!artifactFile.startsWith(storageRoot)) {
                throw new IllegalArgumentException("artifact path escapes storage root");
            }
            Files.write(artifactFile, contentBytes);

            String relativeLocation = sharedRoot.relativize(artifactFile).toString().replace("\\", "/");
            return new ArtifactContentPointer(
                    WORKSPACE_STORAGE_KIND,
                    relativeLocation,
                    safeMediaType,
                    contentBytes.length
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store artifact content", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "content.txt";
        }
        String safe = fileName.trim().replace("\\", "_").replace("/", "_");
        return safe.isBlank() ? "content.txt" : safe;
    }
}
