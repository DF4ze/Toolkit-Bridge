package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisReviewWorkflowRunnerResumeTest {

    @TempDir
    Path tempDir;

    @Test
    void runCorrectionAfterReviewExecutesCorrectionForManualResumeWithHumanNote() throws Exception {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep noopReview = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "review", Map.of("requiresCorrection", false));
        WorkflowStep correctionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "Correction completed",
                Map.of("resultArtifactPath", "result.md")
        );

        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, noopReview, correctionStep);
        WorkflowExecutionContext context = buildContext(Map.of(
                "humanDecisionNote", "Review updated manually by reviewer",
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase4",
                "stepNumber", 4
        ));

        WorkflowStepResult result = runner.runCorrectionAfterReview(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Correction completed");
        assertThat(result.data()).containsKeys("resultArtifactPath", "workflowSummaryPath");

        Path summaryPath = Path.of(result.data().get("workflowSummaryPath").toString());
        assertThat(Files.exists(summaryPath)).isTrue();
        assertThat(Files.readString(summaryPath, StandardCharsets.UTF_8))
                .contains("Decision: CONTINUE")
                .contains("Correction triggered: true");
    }

    @Test
    void runAnalysisReviewWithOptionalCorrectionGeneratesSummaryWhenReviewWaitsHuman() throws Exception {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep waitHumanReview = context -> new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                "Global review requires human decision",
                Map.of(
                        "waitReason", "Missing decision in review",
                        "reviewResultPath", "result.4.review.md",
                        "resultArtifactPath", "result.4.review.md"
                )
        );
        WorkflowStep correctionStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "Correction completed", Map.of());

        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, waitHumanReview, correctionStep);
        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase4",
                "stepNumber", 4
        ));

        WorkflowStepResult result = runner.runAnalysisReviewWithOptionalCorrection(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.data()).containsKey("workflowSummaryPath");

        Path summaryPath = Path.of(result.data().get("workflowSummaryPath").toString());
        assertThat(Files.readString(summaryPath, StandardCharsets.UTF_8))
                .contains("Decision: WAIT_HUMAN")
                .contains("Reason: Missing decision in review")
                .contains("Edit review result");
    }

    @Test
    void runCorrectionAfterReviewReturnsStopFailureWhenCorrectionFails() {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep noopReview = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "review", Map.of("requiresCorrection", false));
        WorkflowStep correctionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "CorrectionStep: Missing required artifact",
                Map.of()
        );

        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, noopReview, correctionStep);
        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase4",
                "stepNumber", 4
        ));

        WorkflowStepResult result = runner.runCorrectionAfterReview(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("CorrectionStep:");
        Path summaryPath = Path.of(result.data().get("workflowSummaryPath").toString());
        assertThat(Files.exists(summaryPath)).isTrue();
    }

    @Test
    void runCorrectionAfterReviewRejectsNullContext() {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep noopReview = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "review", Map.of("requiresCorrection", false));
        WorkflowStep correctionStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "Correction completed", Map.of());
        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, noopReview, correctionStep);

        assertThatThrownBy(() -> runner.runCorrectionAfterReview(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void runCorrectionAfterReviewDoesNotFailWhenSummaryWriteFails() {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = mock(WorkflowArtifactService.class);
        when(artifactService.buildArtifactPath(any(), any(), any(), any(Integer.class), any()))
                .thenReturn(tempDir.resolve("data/rapport/v1.1/CodexTime/Phase4/workflow-summary.md"));
        doThrow(new IllegalStateException("summary write failed"))
                .when(artifactService).writeArtifact(any(), any());

        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep noopReview = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "review", Map.of("requiresCorrection", false));
        WorkflowStep correctionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "Correction completed",
                Map.of("resultArtifactPath", "result.md")
        );

        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, noopReview, correctionStep);
        WorkflowExecutionContext context = buildContext(Map.of(
                "reportRootDirectory", tempDir.resolve("data/rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/Phase4",
                "stepNumber", 4
        ));

        WorkflowStepResult result = runner.runCorrectionAfterReview(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Correction completed");
        assertThat(result.data()).doesNotContainKey("workflowSummaryPath");
    }

    @Test
    void runCorrectionAfterReviewSkipsSummaryWhenReportingVariablesAreMissing() {
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();
        WorkflowArtifactService artifactService = mock(WorkflowArtifactService.class);
        WorkflowStep noopAnalysis = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep noopReview = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "review", Map.of("requiresCorrection", false));
        WorkflowStep correctionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "Correction completed",
                Map.of("resultArtifactPath", "result.md")
        );

        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(orchestrator, artifactService, noopAnalysis, noopReview, correctionStep);

        WorkflowStepResult result = runner.runCorrectionAfterReview(buildContext(Map.of()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Correction completed");
        assertThat(result.data()).doesNotContainKey("workflowSummaryPath");
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-4/step-3",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, variables);
    }
}
