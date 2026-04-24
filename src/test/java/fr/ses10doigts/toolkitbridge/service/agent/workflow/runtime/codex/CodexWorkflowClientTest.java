package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodexWorkflowClientTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsNullRequest() {
        CodexWorkflowClient client = new CodexWorkflowClient();

        assertThatThrownBy(() -> client.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    @Test
    void rejectsMissingWorkingDirectory() {
        CodexWorkflowClient client = new CodexWorkflowClient();
        Path missingDir = tempDir.resolve("missing-dir");
        CodexExecutionRequest request = new CodexExecutionRequest("prompt", missingDir, 10);

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workingDirectory must exist");
    }

    @Test
    void rejectsWorkingDirectoryWhenPathIsNotDirectory() throws Exception {
        CodexWorkflowClient client = new CodexWorkflowClient();
        Path filePath = tempDir.resolve("not-a-dir.txt");
        Files.writeString(filePath, "x");
        CodexExecutionRequest request = new CodexExecutionRequest("prompt", filePath, 10);

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workingDirectory must be a directory");
    }
}
