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

class GlobalReviewStepTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsContinueAndWritesArtifactsOnNominalCase() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review result",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global review completed");
        assertThat(result.data())
                .containsKeys("promptArtifactPath", "resultArtifactPath", "requiresCorrection", "finalDecision", "correctionTriggered", "nextAction")
                .containsEntry("requiresCorrection", false)
                .containsEntry("finalDecision", "CONTINUE")
                .containsEntry("correctionTriggered", false);

        Path promptPath = Path.of(result.data().get("promptArtifactPath").toString());
        Path resultPath = Path.of(result.data().get("resultArtifactPath").toString());

        assertThat(Files.exists(promptPath)).isTrue();
        assertThat(Files.exists(resultPath)).isTrue();
        assertThat(Files.readString(resultPath, StandardCharsets.UTF_8)).isEqualTo("Review result");
    }

    @Test
    void returnsWaitHumanWhenReviewResultContainsWaitHumanDecisionMarker() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary\nDECISION: WAIT_HUMAN\nWAIT_REASON: Missing business validation",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.message()).isEqualTo("Global review requires human decision");
        assertThat(result.data())
                .containsKeys("promptArtifactPath", "resultArtifactPath", "waitReason", "finalDecision", "correctionTriggered", "nextAction")
                .containsEntry("waitReason", "Missing business validation")
                .containsEntry("finalDecision", "WAIT_HUMAN")
                .containsEntry("correctionTriggered", false);
    }

    @Test
    void returnsWaitHumanWithoutWaitReasonWhenMarkerIsPresentWithoutReason() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary\nDECISION: WAIT_HUMAN",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.message()).isEqualTo("Global review requires human decision");
        assertThat(result.data()).containsKeys("promptArtifactPath", "resultArtifactPath");
        assertThat(result.data()).doesNotContainKey("waitReason");
        assertThat(result.data()).containsEntry("finalDecision", "WAIT_HUMAN");
    }

    @Test
    void returnsContinueWhenReviewResultDoesNotContainDecisionMarker() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary without manual intervention requirement",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global review completed");
        assertThat(result.data()).containsEntry("requiresCorrection", false);
        assertThat(result.data()).containsEntry("finalDecision", "CONTINUE");
    }

    @Test
    void returnsContinueWithRequiresCorrectionTrueWhenNeedCorrectionMarkerIsPresent() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary\nDECISION: NEED_CORRECTION",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global review completed");
        assertThat(result.data()).containsEntry("requiresCorrection", true);
        assertThat(result.data()).containsEntry("finalDecision", "CONTINUE");
    }

    @Test
    void returnsContinueWithRequiresCorrectionFalseWhenOkMarkerIsPresent() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary\nDECISION: OK",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global review completed");
        assertThat(result.data()).containsEntry("requiresCorrection", false);
        assertThat(result.data()).containsEntry("finalDecision", "CONTINUE");
    }

    @Test
    void appliesDirectivePriorityNeedCorrectionOverOk() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "DECISION: OK\nDECISION: NEED_CORRECTION",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.data()).containsEntry("requiresCorrection", true);
    }

    @Test
    void appliesDirectivePriorityWaitHumanOverNeedCorrection() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "DECISION: NEED_CORRECTION\nDECISION: WAIT_HUMAN",
                "",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.message()).isEqualTo("Global review requires human decision");
    }

    @Test
    void ignoresWaitHumanMarkerWhenPresentOnlyInStderr() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Review summary without decision marker",
                "DECISION: WAIT_HUMAN",
                true,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Global review completed");
        assertThat(result.data()).containsEntry("requiresCorrection", false);
    }

    @Test
    void returnsStopFailureWhenRequiredVariableIsMissing() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3"
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalReviewStep:")
                .contains("Missing required variable: stepNumber");
    }

    @Test
    void returnsStopFailureWhenAnalysisArtifactIsMissing() {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalReviewStep:")
                .contains("Missing required artifact:");
    }

    @Test
    void returnsStopFailureWhenCodexExecutionFails() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenThrow(new CodexExecutionException("boom", new RuntimeException("x")));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalReviewStep:")
                .contains("boom");
    }

    @Test
    void returnsStopFailureAndKeepsArtifactsWhenCodexResultIsNotSuccessful() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                1,
                "partial review output",
                "review failure details",
                false,
                false,
                10L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalReviewStep:")
                .contains("Codex execution was not successful");
        assertThat(result.data()).containsEntry("finalDecision", "STOP_FAILURE");

        Path promptPath = Path.of(result.data().get("promptArtifactPath").toString());
        Path resultPath = Path.of(result.data().get("resultArtifactPath").toString());
        assertThat(Files.exists(promptPath)).isTrue();
        assertThat(Files.exists(resultPath)).isTrue();
    }

    @Test
    void returnsStopFailureWhenCodexExecutionTimesOut() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

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
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message())
                .contains("GlobalReviewStep:")
                .contains("Codex execution timed out");
        assertThat(result.data()).containsEntry("finalDecision", "STOP_FAILURE");
    }

    @Test
    void writesPromptAndResultArtifacts() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        GlobalReviewStep step = new GlobalReviewStep(codexClient, artifactService);

        Path reportRoot = tempDir.resolve("data/rapport");
        writeAnalysisArtifacts(artifactService, reportRoot, "v1.1", "CodexTime/Phase3", 1);

        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex prompt",
                0,
                "Codex review output",
                "",
                true,
                false,
                20L
        ));

        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", reportRoot,
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase3",
                "stepNumber", 1
        ));

        WorkflowStepResult result = step.execute(context);

        Path promptPath = Path.of(result.data().get("promptArtifactPath").toString());
        Path resultPath = Path.of(result.data().get("resultArtifactPath").toString());

        assertThat(Files.readString(promptPath, StandardCharsets.UTF_8))
                .contains("Global review request")
                .contains("[analysis_prompt]")
                .contains("[analysis_result]");
        assertThat(Files.readString(resultPath, StandardCharsets.UTF_8)).contains("Codex review output");
    }

    private void writeAnalysisArtifacts(WorkflowArtifactService artifactService,
                                        Path reportRoot,
                                        String version,
                                        String phase,
                                        int stepNumber) {
        Path analysisPromptPath = artifactService.buildArtifactPath(
                reportRoot, version, phase, stepNumber, WorkflowArtifactType.ANALYSIS_PROMPT
        );
        Path analysisResultPath = artifactService.buildArtifactPath(
                reportRoot, version, phase, stepNumber, WorkflowArtifactType.ANALYSIS_RESULT
        );
        artifactService.writeArtifact(analysisPromptPath, "Analysis prompt content");
        artifactService.writeArtifact(analysisResultPath, "Analysis result content");
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-3/step-1",
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
