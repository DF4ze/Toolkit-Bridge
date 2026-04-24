package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionException;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrectionStepTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsContinueAndWritesArtifactsOnNominalCase() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CorrectionStep step = new CorrectionStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeInputArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 4);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Corrected content",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 4
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Correction completed");
        assertThat(result.data())
                .containsKeys("promptArtifactPath", "resultArtifactPath", "finalDecision", "correctionTriggered", "nextAction")
                .containsEntry("finalDecision", "CONTINUE")
                .containsEntry("correctionTriggered", true);

        Path promptPath = Path.of(result.data().get("promptArtifactPath").toString());
        Path resultPath = Path.of(result.data().get("resultArtifactPath").toString());

        assertThat(Files.exists(promptPath)).isTrue();
        assertThat(Files.exists(resultPath)).isTrue();
        assertThat(Files.readString(promptPath, StandardCharsets.UTF_8))
                .contains("Analysis result")
                .contains("Review result")
                .contains("Provide a corrected version based on the review");
        assertThat(Files.readString(resultPath, StandardCharsets.UTF_8)).isEqualTo("Corrected content");
    }

    @Test
    void returnsStopFailureWhenRequiredArtifactIsMissing() {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CorrectionStep step = new CorrectionStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        Path analysisResultPath = artifactService.buildArtifactPath(
                reportRoot, "v1.1", "CodexTime/Phase3", 4, WorkflowArtifactType.ANALYSIS_RESULT
        );
        artifactService.writeArtifact(analysisResultPath, "Analysis only");

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 4
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("CorrectionStep:")
                .contains("Missing required artifact:");
        assertThat(result.data()).containsEntry("correctionTriggered", false);
    }

    @Test
    void returnsStopFailureWhenCodexExecutionFails() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CorrectionStep step = new CorrectionStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeInputArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 4);

        when(codexClient.execute(any())).thenThrow(new CodexExecutionException("boom", new RuntimeException("x")));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 4
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("CorrectionStep:")
                .contains("boom");
    }

    @Test
    void returnsStopFailureWhenCodexResultIsNotSuccessful() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CorrectionStep step = new CorrectionStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeInputArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 4);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                1,
                "partial correction",
                "failure details",
                false,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 4
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("CorrectionStep:")
                .contains("Codex execution was not successful");
        assertThat(result.data())
                .containsKeys("promptArtifactPath", "resultArtifactPath", "finalDecision", "correctionTriggered", "nextAction")
                .containsEntry("finalDecision", "STOP_FAILURE")
                .containsEntry("correctionTriggered", true);
    }

    @Test
    void returnsStopFailureWhenCodexExecutionTimesOut() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CorrectionStep step = new CorrectionStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeInputArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 4);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                null,
                "",
                "",
                false,
                true,
                120_000L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 4
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("CorrectionStep:")
                .contains("Codex execution timed out");
        assertThat(result.data())
                .containsKeys("promptArtifactPath", "resultArtifactPath", "finalDecision", "correctionTriggered", "nextAction")
                .containsEntry("finalDecision", "STOP_FAILURE")
                .containsEntry("correctionTriggered", true);
    }

    private void writeInputArtifacts(WorkflowArtifactService artifactService,
                                     Path reportRoot,
                                     String version,
                                     String phase,
                                     int stepNumber) {
        Path analysisResultPath = artifactService.buildArtifactPath(
                reportRoot, version, phase, stepNumber, WorkflowArtifactType.ANALYSIS_RESULT
        );
        Path reviewResultPath = artifactService.buildArtifactPath(
                reportRoot, version, phase, stepNumber, WorkflowArtifactType.REVIEW_RESULT
        );
        artifactService.writeArtifact(analysisResultPath, "Analysis result content");
        artifactService.writeArtifact(reviewResultPath, "Review result content");
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-3/step-4",
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
