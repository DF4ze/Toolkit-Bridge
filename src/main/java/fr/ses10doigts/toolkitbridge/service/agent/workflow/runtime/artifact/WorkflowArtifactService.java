package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class WorkflowArtifactService {

    public Path buildArtifactPath(Path rootDirectory,
                                  String version,
                                  String phase,
                                  int stepNumber,
                                  WorkflowArtifactType type) {
        Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        if (phase == null || phase.isBlank()) {
            throw new IllegalArgumentException("phase must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");

        String fileName = type.fileNameForStep(stepNumber);
        return rootDirectory
                .resolve(version.trim())
                .resolve(phase.trim())
                .resolve(fileName);
    }

    public void writeArtifact(Path artifactPath, String content) {
        Objects.requireNonNull(artifactPath, "artifactPath must not be null");
        String safeContent = content == null ? "" : content;

        try {
            Path parent = artifactPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(artifactPath, safeContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write workflow artifact: " + artifactPath, e);
        }
    }

    public String readArtifact(Path artifactPath) {
        Objects.requireNonNull(artifactPath, "artifactPath must not be null");
        try {
            return Files.readString(artifactPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read workflow artifact: " + artifactPath, e);
        }
    }

    public boolean artifactExists(Path artifactPath) {
        Objects.requireNonNull(artifactPath, "artifactPath must not be null");
        return Files.exists(artifactPath);
    }
}
