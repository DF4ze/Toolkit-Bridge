package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void buildCommandUsesCodexExecAndStdinWithWorkingDirectory() throws Exception {
        CodexWorkflowClient client = new CodexWorkflowClient();
        Path workDir = tempDir.resolve("my project");
        Files.createDirectories(workDir);
        CodexExecutionRequest request = new CodexExecutionRequest("hello", workDir, 10);

        List<String> command = client.buildCommand(request);

        assertThat(command).isNotEmpty();
//        assertThat(command.get(0)).isEqualTo("codex");
        assertThat(command).contains("exec");
        assertThat(command).contains("--cd");
        int cdIndex = command.indexOf("--cd");
        assertThat(cdIndex).isGreaterThanOrEqualTo(0);
        assertThat(command).hasSizeGreaterThan(cdIndex + 1);
        assertThat(command.get(cdIndex + 1)).isEqualTo(workDir.toString());
        assertThat(command.get(command.size() - 1)).isEqualTo("-");
        assertThat(command).doesNotContain("hello");
        assertThat(command).doesNotContain("cmd", "/c");
    }

    @Test
    void buildCommandUsesCodexExecAndStdinWithoutWorkingDirectory() {
        CodexWorkflowClient client = new CodexWorkflowClient();
        CodexExecutionRequest request = new CodexExecutionRequest("hello", null, 10);

        List<String> command = client.buildCommand(request);

        assertThat(command).isNotEmpty();
//        assertThat(command.get(0)).isEqualTo("codex");
        assertThat(command).contains("exec");
        assertThat(command).doesNotContain("--cd");
        assertThat(command.get(command.size() - 1)).isEqualTo("-");
        assertThat(command).doesNotContain("hello");
        assertThat(command).doesNotContain("cmd", "/c");
    }
}
