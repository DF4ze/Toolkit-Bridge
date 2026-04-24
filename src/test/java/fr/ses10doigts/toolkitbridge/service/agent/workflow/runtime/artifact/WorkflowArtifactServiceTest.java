package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowArtifactServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsArtifactPathFromSimpleContext() {
        WorkflowArtifactService service = new WorkflowArtifactService();

        Path artifactPath = service.buildArtifactPath(
                tempDir.resolve("data/rapport"),
                "v1.1",
                "CodexTime/Phase1",
                4,
                WorkflowArtifactType.ANALYSIS_RESULT
        );

        assertThat(artifactPath.toString().replace("\\", "/"))
                .endsWith("data/rapport/v1.1/CodexTime/Phase1/result.4.analysis.md");
    }

    @Test
    void writesReadsAndChecksArtifactExistence() throws Exception {
        WorkflowArtifactService service = new WorkflowArtifactService();
        Path artifactPath = tempDir.resolve("data/rapport/v1.1/CodexTime/Phase1/result.4.implements.md");

        assertThat(service.artifactExists(artifactPath)).isFalse();
        service.writeArtifact(artifactPath, "Implementation report");

        assertThat(service.artifactExists(artifactPath)).isTrue();
        assertThat(Files.exists(artifactPath.getParent())).isTrue();
        assertThat(service.readArtifact(artifactPath)).isEqualTo("Implementation report");
    }
}
