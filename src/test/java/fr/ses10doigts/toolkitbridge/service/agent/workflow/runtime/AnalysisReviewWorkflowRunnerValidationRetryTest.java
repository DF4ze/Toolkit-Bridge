package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.MavenValidationStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.WorkflowValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisReviewWorkflowRunnerValidationRetryTest {

    @TempDir
    Path tempDir;

    @Test
    void runWithValidationAndRetryCorrectsBuildFailureThenSucceeds() {
        AtomicInteger validationCount = new AtomicInteger(0);
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> {
            if (validationCount.getAndIncrement() == 0) {
                return new WorkflowStepResult(
                        WorkflowStepDecision.RETRY_CORRECTION,
                        "Build failed. Retry count: 1/2",
                        Map.of(
                                "buildErrorRetryCount", 1,
                                "buildErrorRetryMax", 2,
                                "buildErrorSummary", "cannot find symbol",
                                "buildArtifactPath", "result.3.build.md",
                                "finalDecision", "RETRY_CORRECTION",
                                "nextAction", "Build failed. Attempting automatic correction..."
                        )
                );
            }
            assertThat(context.variables()).containsEntry("buildErrorRetryCount", 1);
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    "Maven build validation passed",
                    Map.of(
                            "buildStatus", "SUCCESS",
                            "finalDecision", "CONTINUE",
                            "nextAction", "Build compiles. Continue workflow execution"
                    )
            );
        };
        WorkflowStep buildErrorCorrectionStep = context -> {
            buildCorrectionCount.incrementAndGet();
            assertThat(context.variables())
                    .containsEntry("buildErrorSummary", "cannot find symbol")
                    .containsEntry("buildArtifactPath", "result.3.build.md");
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    "Build error correction attempted",
                    Map.of("finalDecision", "CONTINUE", "nextAction", "Rerun Maven validation")
            );
        };

        AnalysisReviewWorkflowRunner runner = buildRunner(validationStep, buildErrorCorrectionStep);

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Maven build validation passed");
        assertThat(validationCount.get()).isEqualTo(2);
        assertThat(buildCorrectionCount.get()).isEqualTo(1);
    }

    @Test
    void runWithValidationAndRetryStopsWhenRetryMaxIsReached() {
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "MavenValidationStep: Build exited with status FAILURE",
                Map.of(
                        "buildStatus", "FAILURE",
                        "buildErrorRetryCount", 2,
                        "buildErrorRetryMax", 2,
                        "finalDecision", "STOP_FAILURE",
                        "nextAction", "Inspect build failure"
                )
        );
        WorkflowStep buildErrorCorrectionStep = context -> {
            buildCorrectionCount.incrementAndGet();
            return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "unexpected", Map.of());
        };

        AnalysisReviewWorkflowRunner runner = buildRunner(validationStep, buildErrorCorrectionStep);

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("Build exited with status FAILURE");
        assertThat(buildCorrectionCount.get()).isEqualTo(0);
    }

    @Test
    void runWithValidationAndRetryConvertsPersistentBuildFailureToWaitHuman() {
        AtomicInteger validationCount = new AtomicInteger(0);
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> {
            if (validationCount.getAndIncrement() == 0) {
                return new WorkflowStepResult(
                        WorkflowStepDecision.RETRY_CORRECTION,
                        "Build failed. Retry count: 1/2",
                        Map.of(
                                "buildStatus", "FAILURE",
                                "buildErrorRetryCount", 1,
                                "buildErrorRetryMax", 2,
                                "buildErrorSummary", "cannot find symbol",
                                "buildArtifactPath", "result.4.build.md",
                                "finalDecision", "RETRY_CORRECTION",
                                "nextAction", "Build failed. Attempting automatic correction..."
                        )
                );
            }
            return new WorkflowStepResult(
                    WorkflowStepDecision.STOP_FAILURE,
                    "MavenValidationStep: Build exited with status FAILURE",
                    Map.of(
                            "buildStatus", "FAILURE",
                            "buildErrorRetryCount", 2,
                            "buildErrorRetryMax", 2,
                            "buildResultPath", "result.4.build.md",
                            "finalDecision", "STOP_FAILURE",
                            "nextAction", "Inspect build failure"
                    )
            );
        };
        WorkflowStep buildErrorCorrectionStep = context -> {
            buildCorrectionCount.incrementAndGet();
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    "Build correction attempted",
                    Map.of(
                            "correctionTriggered", true,
                            "correctionAttemptPath", "result.4.build-error-correction.md",
                            "finalDecision", "CONTINUE",
                            "nextAction", "Rerun Maven validation"
                    )
            );
        };

        AnalysisReviewWorkflowRunner runner = buildRunner(validationStep, buildErrorCorrectionStep);

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.message()).isEqualTo("Build still fails after automatic correction attempts");
        assertThat(result.data())
                .containsEntry("finalDecision", "WAIT_HUMAN")
                .containsEntry("waitReason", "Build still fails after automatic correction attempts")
                .containsEntry("nextAction", "Inspect build artifact and correction attempt, then decide how to proceed")
                .containsEntry("correctionTriggered", true);
        assertThat(validationCount.get()).isEqualTo(2);
        assertThat(buildCorrectionCount.get()).isEqualTo(1);
    }

    @Test
    void workflowSummaryUsesBuildInstructionsForWaitHumanBuildFailure() throws Exception {
        AtomicInteger validationCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> {
            if (validationCount.getAndIncrement() == 0) {
                return new WorkflowStepResult(
                        WorkflowStepDecision.RETRY_CORRECTION,
                        "Build failed. Retry count: 1/2",
                        Map.of(
                                "buildStatus", "FAILURE",
                                "buildErrorRetryCount", 1,
                                "buildErrorRetryMax", 2,
                                "buildErrorSummary", "cannot find symbol",
                                "buildArtifactPath", "result.4.build.md",
                                "finalDecision", "RETRY_CORRECTION",
                                "nextAction", "Build failed. Attempting automatic correction..."
                        )
                );
            }
            return new WorkflowStepResult(
                    WorkflowStepDecision.STOP_FAILURE,
                    "MavenValidationStep: Build exited with status FAILURE",
                    Map.of(
                            "buildStatus", "FAILURE",
                            "buildErrorRetryCount", 2,
                            "buildErrorRetryMax", 2,
                            "buildResultPath", "result.4.build.md",
                            "finalDecision", "STOP_FAILURE",
                            "nextAction", "Inspect build failure"
                    )
            );
        };
        WorkflowStep buildErrorCorrectionStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "Build correction attempted",
                Map.of(
                        "correctionTriggered", true,
                        "correctionAttemptPath", "result.4.build-error-correction.md",
                        "finalDecision", "CONTINUE",
                        "nextAction", "Rerun Maven validation"
                )
        );

        AnalysisReviewWorkflowRunner runner = buildRunner(validationStep, buildErrorCorrectionStep);

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContextWithSummary());

        Path summaryPath = Path.of(result.data().get("workflowSummaryPath").toString());
        String summary = Files.readString(summaryPath);
        assertThat(summary)
                .contains("Status: WAIT_HUMAN")
                .contains("Reason: Build still fails after automatic correction attempts")
                .contains("Context:")
                .contains("Actions:")
                .contains("Artifacts:");
    }

    @Test
    void runWithValidationAndRetryDoesNotRetryTimeout() {
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "MavenValidationStep: Process timed out",
                Map.of("buildStatus", "TIMEOUT", "finalDecision", "STOP_FAILURE", "nextAction", "Inspect timeout")
        );
        AnalysisReviewWorkflowRunner runner = buildRunner(
                validationStep,
                context -> {
                    buildCorrectionCount.incrementAndGet();
                    return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "unexpected", Map.of());
                }
        );

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.data()).containsEntry("buildStatus", "TIMEOUT");
        assertThat(buildCorrectionCount.get()).isEqualTo(0);
    }

    @Test
    void runWithValidationAndRetryDoesNotRetrySystemError() {
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                "MavenValidationStep: Failed to start process",
                Map.of("buildStatus", "SYSTEM_ERROR", "finalDecision", "STOP_FAILURE", "nextAction", "Inspect system error")
        );
        AnalysisReviewWorkflowRunner runner = buildRunner(
                validationStep,
                context -> {
                    buildCorrectionCount.incrementAndGet();
                    return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "unexpected", Map.of());
                }
        );

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.data()).containsEntry("buildStatus", "SYSTEM_ERROR");
        assertThat(buildCorrectionCount.get()).isEqualTo(0);
    }

    @Test
    void runWithValidationAndRetryStopsOnInvalidRetryState() {
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.RETRY_CORRECTION,
                "Build failed without retry metadata",
                Map.of(
                        "buildErrorSummary", "cannot find symbol",
                        "buildArtifactPath", "result.3.build.md",
                        "finalDecision", "RETRY_CORRECTION",
                        "nextAction", "Build failed. Attempting automatic correction..."
                )
        );
        AnalysisReviewWorkflowRunner runner = buildRunner(
                validationStep,
                context -> {
                    buildCorrectionCount.incrementAndGet();
                    return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "unexpected", Map.of());
                }
        );

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("Invalid build retry state");
        assertThat(result.data()).containsEntry("finalDecision", "STOP_FAILURE");
        assertThat(buildCorrectionCount.get()).isEqualTo(0);
    }

    @Test
    void runWithValidationAndRetryStopsWhenRetryStateReachedMaxBeforeCorrection() {
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep validationStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.RETRY_CORRECTION,
                "Build failed. Retry count: 2/2",
                Map.of(
                        "buildErrorRetryCount", 2,
                        "buildErrorRetryMax", 2,
                        "buildErrorSummary", "cannot find symbol",
                        "buildArtifactPath", "result.3.build.md",
                        "finalDecision", "RETRY_CORRECTION",
                        "nextAction", "Build failed. Attempting automatic correction..."
                )
        );
        AnalysisReviewWorkflowRunner runner = buildRunner(
                validationStep,
                context -> {
                    buildCorrectionCount.incrementAndGet();
                    return new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "unexpected", Map.of());
                }
        );

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildContext());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("Build retry limit reached");
        assertThat(buildCorrectionCount.get()).isEqualTo(0);
    }

    @Test
    void runWithValidationAndRetryConvertsRealMavenRetryLimitFlowToWaitHuman() {
        WorkflowValidationService validationService = mock(WorkflowValidationService.class);
        when(validationService.validate(any()))
                .thenReturn(new ValidationResult(
                        "cmd",
                        ValidationStatus.FAILURE,
                        1,
                        "build output",
                        "cannot find symbol",
                        100L,
                        null
                ))
                .thenReturn(new ValidationResult(
                        "cmd",
                        ValidationStatus.FAILURE,
                        1,
                        "build output",
                        "still cannot find symbol",
                        120L,
                        null
                ));

        WorkflowStep validationStep = new MavenValidationStep(validationService, new WorkflowArtifactService());
        AtomicInteger buildCorrectionCount = new AtomicInteger(0);
        WorkflowStep buildErrorCorrectionStep = context -> {
            buildCorrectionCount.incrementAndGet();
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    "Build correction attempted",
                    Map.of(
                            "correctionAttemptPath", "result.4.build-error-correction.md",
                            "finalDecision", "CONTINUE",
                            "nextAction", "Rerun Maven validation"
                    )
            );
        };

        AnalysisReviewWorkflowRunner runner = buildRunner(validationStep, buildErrorCorrectionStep);

        WorkflowStepResult result = runner.runWithValidationAndRetry(buildBuildContextWithValidation());

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.WAIT_HUMAN);
        assertThat(result.data())
                .containsEntry("finalDecision", "WAIT_HUMAN")
                .containsEntry("waitReason", "Build still fails after automatic correction attempts")
                .containsEntry("buildStatus", "FAILURE")
                .containsEntry("correctionTriggered", true)
                .containsEntry("buildErrorRetryCount", 2)
                .containsEntry("buildErrorRetryMax", 2);
        assertThat(buildCorrectionCount.get()).isEqualTo(1);
    }

    private AnalysisReviewWorkflowRunner buildRunner(WorkflowStep validationStep, WorkflowStep buildErrorCorrectionStep) {
        WorkflowStep analysisStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "analysis", Map.of());
        WorkflowStep reviewStep = context -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "review",
                Map.of("requiresCorrection", false)
        );
        WorkflowStep correctionStep = context -> new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "correction", Map.of());
        return new AnalysisReviewWorkflowRunner(
                new WorkflowOrchestrator(),
                new WorkflowArtifactService(),
                analysisStep,
                reviewStep,
                correctionStep,
                validationStep,
                buildErrorCorrectionStep
        );
    }

    private WorkflowExecutionContext buildContext() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-test",
                "codex-implementation",
                "phase-6/step-3",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, Map.of());
    }

    private WorkflowExecutionContext buildContextWithSummary() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-test",
                "codex-implementation",
                "phase-6/step-4",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, Map.of(
                "reportRootDirectory", tempDir.resolve("rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/phase6/etape4",
                "stepNumber", 4
        ));
    }

    private WorkflowExecutionContext buildBuildContextWithValidation() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-test",
                "codex-implementation",
                "phase-6/step-4",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        return new WorkflowExecutionContext(workflowRun, null, Map.of(
                "reportRootDirectory", tempDir.resolve("rapport"),
                "reportVersion", "v1.1",
                "reportPhase", "CodexTime/phase6/etape4",
                "stepNumber", 4,
                "validationWorkingDirectory", tempDir
        ));
    }
}
