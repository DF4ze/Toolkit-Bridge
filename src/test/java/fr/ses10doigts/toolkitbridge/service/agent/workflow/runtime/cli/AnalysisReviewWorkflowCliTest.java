package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.AnalysisReviewWorkflowRunner;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisReviewWorkflowCliTest {

    @TempDir
    Path tempDir;

    @Test
    void executeRunCallsAnalysisReviewRunnerAndPrintsExternalContract() {
        AtomicBoolean analysisCalled = new AtomicBoolean(false);
        AtomicBoolean reviewCalled = new AtomicBoolean(false);
        WorkflowStep analysisStep = context -> {
            analysisCalled.set(true);
            assertThat(context.variables()).containsKeys(
                    "reportRootDirectory",
                    "reportVersion",
                    "reportPhase",
                    "stepNumber",
                    "analysisSourcePath"
            );
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        };
        WorkflowStep reviewStep = context -> {
            reviewCalled.set(true);
            return new WorkflowStepResult(
                    WorkflowStepDecision.WAIT_HUMAN,
                    "Global review requires human decision",
                    Map.of(
                            "waitReason", "Missing decision in review",
                            "reviewResultPath", "result.1.review.md"
                    )
            );
        };
        WorkflowStep correctionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "Correction should not run",
                Map.of()
        );
        CliHarness harness = buildHarness(analysisStep, reviewStep, correctionStep);

        int exitCode = harness.cli().execute(new String[]{
                "--mode=RUN",
                "--reportRootDirectory=" + tempDir.resolve("data/rapport"),
                "--reportVersion=v1.1",
                "--reportPhase=CodexTime/phase5",
                "--stepNumber=1",
                "--analysisSourcePath=" + tempDir.resolve("1.analysis.md")
        });

        assertThat(exitCode).isZero();
        assertThat(analysisCalled).isTrue();
        assertThat(reviewCalled).isTrue();
        assertThat(harness.output())
                .contains("decision=WAIT_HUMAN")
                .contains("message=Global review requires human decision")
                .contains("finalDecision=WAIT_HUMAN")
                .contains("nextAction=")
                .contains("correctionTriggered=false")
                .contains("waitReason=Missing decision in review")
                .contains("workflowSummaryPath=");
    }

    @Test
    void executeResumeCallsCorrectionRunnerAndPrintsExternalContract() {
        AtomicBoolean correctionCalled = new AtomicBoolean(false);
        WorkflowStep analysisStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep reviewStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "review",
                Map.of("requiresCorrection", false)
        );
        WorkflowStep correctionStep = context -> {
            correctionCalled.set(true);
            assertThat(context.variables()).doesNotContainKey("analysisSourcePath");
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    "Correction completed",
                    Map.of(
                            "correctionResultPath", "result.1.correction.md",
                            "nextAction", "Correction done. Continue workflow validation or next step"
                    )
            );
        };
        CliHarness harness = buildHarness(analysisStep, reviewStep, correctionStep);

        int exitCode = harness.cli().execute(new String[]{
                "--mode=RESUME",
                "--reportRootDirectory=" + tempDir.resolve("data/rapport"),
                "--reportVersion=v1.1",
                "--reportPhase=CodexTime/phase5",
                "--stepNumber=1"
        });

        assertThat(exitCode).isZero();
        assertThat(correctionCalled).isTrue();
        assertThat(harness.output())
                .contains("decision=CONTINUE")
                .contains("message=Correction completed")
                .contains("finalDecision=CONTINUE")
                .contains("nextAction=Correction done. Continue workflow validation or next step")
                .contains("correctionTriggered=true")
                .contains("workflowSummaryPath=");
    }

    @Test
    void executePrintsNoSummaryPathWhenRunnerDoesNotProvideOne() {
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runCorrectionAfterReview(any(WorkflowExecutionContext.class)))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.CONTINUE,
                        "Correction completed",
                        Map.of(
                                "finalDecision", "CONTINUE",
                                "nextAction", "Continue workflow execution",
                                "correctionTriggered", true
                        )
                ));
        CliHarness harness = buildHarness(runner);

        int exitCode = harness.cli().execute(new String[]{
                "--mode=RESUME",
                "--reportRootDirectory=" + tempDir.resolve("data/rapport"),
                "--reportVersion=v1.1",
                "--reportPhase=CodexTime/phase5",
                "--stepNumber=1"
        });

        assertThat(exitCode).isZero();
        assertThat(harness.output())
                .contains("decision=CONTINUE")
                .contains("correctionTriggered=true")
                .doesNotContain("workflowSummaryPath=");
    }

    @Test
    void executePrintsStopFailureWithMandatoryFallbacksAndSingleLineValues() {
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runCorrectionAfterReview(any(WorkflowExecutionContext.class)))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        "Correction failed\nMissing artifact",
                        Map.of()
                ));
        CliHarness harness = buildHarness(runner);

        int exitCode = harness.cli().execute(new String[]{
                "--mode=RESUME",
                "--reportRootDirectory=" + tempDir.resolve("data/rapport"),
                "--reportVersion=v1.1",
                "--reportPhase=CodexTime/phase5",
                "--stepNumber=1"
        });

        assertThat(exitCode).isZero();
        assertThat(harness.output())
                .contains("decision=STOP_FAILURE")
                .contains("message=Correction failed Missing artifact")
                .contains("finalDecision=STOP_FAILURE")
                .contains("nextAction=Inspect failure and related artifacts, then rerun")
                .contains("correctionTriggered=false");
    }

    @Test
    void executeRunRequiresAnalysisSourcePath() {
        WorkflowStep noopStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "noop", Map.of());
        CliHarness harness = buildHarness(noopStep, noopStep, noopStep);

        int exitCode = harness.cli().execute(new String[]{
                "--mode=RUN",
                "--reportRootDirectory=" + tempDir.resolve("data/rapport"),
                "--reportVersion=v1.1",
                "--reportPhase=CodexTime/phase5",
                "--stepNumber=1"
        });

        assertThat(exitCode).isEqualTo(2);
        assertThat(harness.error()).contains("Missing required argument: analysisSourcePath");
    }

    private CliHarness buildHarness(WorkflowStep analysisStep, WorkflowStep reviewStep, WorkflowStep correctionStep) {
        WorkflowStep noopValidation = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE, "validation",
                java.util.Map.of("buildStatus", "SUCCESS", "buildDurationMs", 0L, "buildResultPath", "build.md"));
        AnalysisReviewWorkflowRunner runner = new AnalysisReviewWorkflowRunner(
                new WorkflowOrchestrator(),
                new WorkflowArtifactService(),
                analysisStep,
                reviewStep,
                correctionStep,
                noopValidation
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        AnalysisReviewWorkflowCli cli = new AnalysisReviewWorkflowCli(
                runner,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)
        );
        return new CliHarness(cli, out, err);
    }

    private CliHarness buildHarness(AnalysisReviewWorkflowRunner runner) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        AnalysisReviewWorkflowCli cli = new AnalysisReviewWorkflowCli(
                runner,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8)
        );
        return new CliHarness(cli, out, err);
    }

    private record CliHarness(AnalysisReviewWorkflowCli cli, ByteArrayOutputStream out, ByteArrayOutputStream err) {

        private String output() {
            return out.toString(StandardCharsets.UTF_8);
        }

        private String error() {
            return err.toString(StandardCharsets.UTF_8);
        }
    }
}
