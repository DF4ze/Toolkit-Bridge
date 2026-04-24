package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionException;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalAnalysisStepTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsContinueAndWritesArtifactsOnNominalCase() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);

        Path sourcePath = tempDir.resolve("source.md");
        Files.writeString(sourcePath, "Source content", java.nio.charset.StandardCharsets.UTF_8);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Analysis result",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase2",
                "stepNumber", 2,
                "analysisSourcePath", sourcePath
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global analysis completed");

        String promptArtifactPath = (String) result.data().get("promptArtifactPath");
        String resultArtifactPath = (String) result.data().get("resultArtifactPath");

        assertThat(promptArtifactPath).isNotBlank();
        assertThat(resultArtifactPath).isNotBlank();
        assertThat(Files.exists(Path.of(promptArtifactPath))).isTrue();
        assertThat(Files.exists(Path.of(resultArtifactPath))).isTrue();
        assertThat(Files.readString(Path.of(resultArtifactPath))).isEqualTo("Analysis result");
    }

    @Test
    void returnsStopFailureWhenRequiredVariableIsMissing() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);

        Path sourcePath = tempDir.resolve("source.md");
        Files.writeString(sourcePath, "Source content", java.nio.charset.StandardCharsets.UTF_8);

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase2",
                "analysisSourcePath", sourcePath
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalAnalysisStep:")
                .contains("Missing required variable: stepNumber");
    }

    @Test
    void returnsStopFailureWhenCodexExecutionFails() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);

        Path sourcePath = tempDir.resolve("source.md");
        Files.writeString(sourcePath, "Source content", java.nio.charset.StandardCharsets.UTF_8);

        when(codexClient.execute(any())).thenThrow(new CodexExecutionException("boom", new RuntimeException("x")));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase2",
                "stepNumber", 2,
                "analysisSourcePath", sourcePath
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalAnalysisStep:")
                .contains("boom");
    }

    @Test
    void returnsStopFailureWhenCodexResultIsNotSuccessfulAndKeepsArtifacts() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);

        Path sourcePath = tempDir.resolve("source.md");
        Files.writeString(sourcePath, "Source content", java.nio.charset.StandardCharsets.UTF_8);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                1,
                "partial output",
                "failure details",
                false,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase2",
                "stepNumber", 2,
                "analysisSourcePath", sourcePath
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalAnalysisStep:")
                .contains("Codex execution was not successful");

        String promptArtifactPath = (String) result.data().get("promptArtifactPath");
        String resultArtifactPath = (String) result.data().get("resultArtifactPath");
        assertThat(promptArtifactPath).isNotBlank();
        assertThat(resultArtifactPath).isNotBlank();
        assertThat(Files.exists(Path.of(promptArtifactPath))).isTrue();
        assertThat(Files.exists(Path.of(resultArtifactPath))).isTrue();
    }

    @Test
    void writesPromptAndResultArtifacts() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);

        Path sourcePath = tempDir.resolve("source.md");
        Files.writeString(sourcePath, "Workflow input", java.nio.charset.StandardCharsets.UTF_8);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Codex output",
                "",
                true,
                false,
                20L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase2",
                "stepNumber", 2,
                "analysisSourcePath", sourcePath
        ));

        WorkflowStepResult result = step.execute(context);

        Path promptPath = Path.of((String) result.data().get("promptArtifactPath"));
        Path resultPath = Path.of((String) result.data().get("resultArtifactPath"));

        assertThat(Files.readString(promptPath)).contains("Workflow input");
        assertThat(Files.readString(resultPath)).contains("Codex output");
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-2/step-2",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, new HashMap<>(variables));
    }
}
