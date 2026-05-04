package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class AnalysisReviewWorkflowRunner {

    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String FINAL_DECISION_KEY = "finalDecision";
    private static final String WAIT_REASON_KEY = "waitReason";
    private static final String CORRECTION_TRIGGERED_KEY = "correctionTriggered";
    private static final String NEXT_ACTION_KEY = "nextAction";
    private static final String WORKFLOW_SUMMARY_PATH_KEY = "workflowSummaryPath";
    private static final String BUILD_ERROR_RETRY_COUNT_KEY = "buildErrorRetryCount";
    private static final String BUILD_ERROR_RETRY_MAX_KEY = "buildErrorRetryMax";
    private static final String BUILD_STATUS_KEY = "buildStatus";
    private static final String REVIEW_RESULT_PATH_KEY = "reviewResultPath";
    private static final String BUILD_ARTIFACT_PATH_KEY = "buildArtifactPath";
    private static final String BUILD_RESULT_PATH_KEY = "buildResultPath";
    private static final String RESULT_ARTIFACT_PATH_KEY = "resultArtifactPath";
    private static final String CORRECTION_ATTEMPT_PATH_KEY = "correctionAttemptPath";
    private static final String BUILD_FAILURE_STATUS = "FAILURE";
    private static final String PERSISTENT_BUILD_FAILURE_WAIT_REASON =
            "Build still fails after automatic correction attempts";
    private static final String PERSISTENT_BUILD_FAILURE_NEXT_ACTION =
            "Inspect build artifact and correction attempt, then decide how to proceed";

    private final WorkflowOrchestrator workflowOrchestrator;
    private final WorkflowArtifactService workflowArtifactService;
    private final WorkflowStep analysisStep;
    private final WorkflowStep reviewStep;
    private final WorkflowStep correctionStep;
    private final WorkflowStep validationStep;
    private final WorkflowStep buildErrorCorrectionStep;

    public AnalysisReviewWorkflowRunner(WorkflowOrchestrator workflowOrchestrator,
                                        WorkflowArtifactService workflowArtifactService,
                                        WorkflowStep analysisStep,
                                        WorkflowStep reviewStep,
                                        WorkflowStep correctionStep,
                                        WorkflowStep validationStep,
                                        WorkflowStep buildErrorCorrectionStep) {
        this.workflowOrchestrator = Objects.requireNonNull(workflowOrchestrator, "workflowOrchestrator must not be null");
        this.workflowArtifactService = Objects.requireNonNull(workflowArtifactService, "workflowArtifactService must not be null");
        this.analysisStep = Objects.requireNonNull(analysisStep, "analysisStep must not be null");
        this.reviewStep = Objects.requireNonNull(reviewStep, "reviewStep must not be null");
        this.correctionStep = Objects.requireNonNull(correctionStep, "correctionStep must not be null");
        this.validationStep = Objects.requireNonNull(validationStep, "validationStep must not be null");
        this.buildErrorCorrectionStep = Objects.requireNonNull(buildErrorCorrectionStep, "buildErrorCorrectionStep must not be null");
    }

    public AnalysisReviewWorkflowRunner(WorkflowOrchestrator workflowOrchestrator,
                                        WorkflowArtifactService workflowArtifactService,
                                        WorkflowStep analysisStep,
                                        WorkflowStep reviewStep,
                                        WorkflowStep correctionStep,
                                        WorkflowStep validationStep) {
        this(
                workflowOrchestrator,
                workflowArtifactService,
                analysisStep,
                reviewStep,
                correctionStep,
                validationStep,
                context -> new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        "Build error correction step is not configured",
                        Map.of()
                )
        );
    }

    public WorkflowStepResult runAnalysisReviewWithOptionalCorrection(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Workflow analysis+review started: runId={}", context.workflowRun().runId());
        WorkflowStepResult result = workflowOrchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );
        return attachWorkflowSummarySafely(context, result);
    }

    public WorkflowStepResult runWithValidation(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Workflow analysis+review+validation started: runId={}", context.workflowRun().runId());
        WorkflowStepResult workflowResult = workflowOrchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );
        if (workflowResult.decision() != WorkflowStepDecision.CONTINUE) {
            return attachWorkflowSummarySafely(context, workflowResult);
        }
        WorkflowStepResult result = validationStep.execute(context);
        return attachWorkflowSummarySafely(context, result);
    }

    public WorkflowStepResult runWithValidationAndRetry(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Workflow run with retry started: runId={}", context.workflowRun().runId());
        WorkflowExecutionContext currentContext = context;

        while (true) {
            WorkflowStepResult result = runWithValidation(currentContext);
            if (result.decision() != WorkflowStepDecision.RETRY_CORRECTION) {
                return convertPersistentBuildFailureToWaitHuman(currentContext, result);
            }

            RetryState retryState = retryState(result);
            if (!retryState.valid()) {
                log.error("Workflow stopped — invalid build retry state: runId={}, error={}",
                        currentContext.workflowRun().runId(), retryState.errorMessage());
                return attachWorkflowSummarySafely(
                        currentContext,
                        stopFailure("Invalid build retry state: " + retryState.errorMessage())
                );
            }
            if (retryState.retryCount() >= retryState.retryMax()) {
                log.warn("Build retry limit reached: runId={}, retryCount={}, retryMax={}",
                        currentContext.workflowRun().runId(), retryState.retryCount(), retryState.retryMax());
                return stopAtRetryLimit(currentContext, result, retryState);
            }

            log.info("Build error correction triggered: runId={}, retryCount={}, retryMax={}",
                    currentContext.workflowRun().runId(), retryState.retryCount(), retryState.retryMax());
            log.debug("Build retry state: runId={}, retryCount={}, retryMax={}",
                    currentContext.workflowRun().runId(), retryState.retryCount(), retryState.retryMax());
            WorkflowExecutionContext correctionContext = withVariables(currentContext, result.data());
            WorkflowStepResult correctionResult = buildErrorCorrectionStep.execute(correctionContext);
            if (correctionResult.decision() != WorkflowStepDecision.CONTINUE) {
                return attachWorkflowSummarySafely(correctionContext, correctionResult);
            }

            Map<String, Object> correctionData = new HashMap<>(correctionResult.data());
            correctionData.put(CORRECTION_TRIGGERED_KEY, true);
            correctionData.put(BUILD_ERROR_RETRY_COUNT_KEY, result.data().getOrDefault(BUILD_ERROR_RETRY_COUNT_KEY, 0));
            correctionData.put(BUILD_ERROR_RETRY_MAX_KEY, result.data().getOrDefault(BUILD_ERROR_RETRY_MAX_KEY, 2));
            currentContext = withVariables(correctionContext, correctionData);
        }
    }

    public WorkflowStepResult runCorrectionAfterReview(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        log.info("Workflow correction after review started: runId={}", context.workflowRun().runId());
        WorkflowStepResult result = workflowOrchestrator.executeSingleStep(context, correctionStep);
        Map<String, Object> data = new HashMap<>(result.data());
        data.put(CORRECTION_TRIGGERED_KEY, true);
        data.putIfAbsent(FINAL_DECISION_KEY, result.decision().name());
        data.putIfAbsent(NEXT_ACTION_KEY, defaultNextAction(result.decision()));
        WorkflowStepResult enrichedResult = new WorkflowStepResult(result.decision(), result.message(), data);
        return attachWorkflowSummarySafely(context, enrichedResult);
    }

    private WorkflowStepResult attachWorkflowSummarySafely(WorkflowExecutionContext context, WorkflowStepResult result) {
        if (!hasSummaryConfiguration(context)) {
            return result;
        }
        try {
            return attachWorkflowSummary(context, result);
        } catch (RuntimeException e) {
            log.warn("Workflow summary generation failed (non-blocking): runId={}", context.workflowRun().runId());
            Map<String, Object> data = new HashMap<>(result.data());
            String fallbackAction = "Summary generation failed; workflow result remains valid. "
                    + toText(data.get(NEXT_ACTION_KEY), defaultNextAction(result.decision()));
            data.put(NEXT_ACTION_KEY, fallbackAction);
            return new WorkflowStepResult(result.decision(), result.message(), data);
        }
    }

    private WorkflowStepResult attachWorkflowSummary(WorkflowExecutionContext context, WorkflowStepResult result) {
        Path summaryPath = workflowArtifactService.buildArtifactPath(
                requiredPath(context, VAR_REPORT_ROOT_DIRECTORY),
                requiredString(context, VAR_REPORT_VERSION),
                requiredString(context, VAR_REPORT_PHASE),
                requiredInt(context, VAR_STEP_NUMBER),
                WorkflowArtifactType.WORKFLOW_SUMMARY
        );

        Map<String, Object> data = new HashMap<>(result.data());
        data.put(WORKFLOW_SUMMARY_PATH_KEY, summaryPath.toString());
        WorkflowStepResult summaryResult = new WorkflowStepResult(result.decision(), result.message(), data);
        String summaryContent = buildSummaryContent(summaryResult);
        workflowArtifactService.writeArtifact(summaryPath, summaryContent);

        data.putIfAbsent(FINAL_DECISION_KEY, result.decision().name());
        data.putIfAbsent(CORRECTION_TRIGGERED_KEY, false);
        data.putIfAbsent(NEXT_ACTION_KEY, defaultNextAction(result.decision()));
        return new WorkflowStepResult(result.decision(), result.message(), data);
    }

    private boolean hasSummaryConfiguration(WorkflowExecutionContext context) {
        Map<String, Object> vars = context.variables();
        return isPresent(vars.get(VAR_REPORT_ROOT_DIRECTORY))
                && isPresent(vars.get(VAR_REPORT_VERSION))
                && isPresent(vars.get(VAR_REPORT_PHASE))
                && isPresent(vars.get(VAR_STEP_NUMBER));
    }

    private WorkflowExecutionContext withVariables(WorkflowExecutionContext context, Map<String, Object> additionalVariables) {
        Map<String, Object> variables = new HashMap<>(context.variables());
        variables.putAll(additionalVariables);
        return new WorkflowExecutionContext(context.workflowRun(), context.lotRun(), variables);
    }

    private RetryState retryState(WorkflowStepResult result) {
        Integer retryCount = optionalInt(result.data().get(BUILD_ERROR_RETRY_COUNT_KEY));
        Integer retryMax = optionalInt(result.data().get(BUILD_ERROR_RETRY_MAX_KEY));
        if (retryCount == null) {
            return RetryState.invalid("missing " + BUILD_ERROR_RETRY_COUNT_KEY);
        }
        if (retryMax == null) {
            return RetryState.invalid("missing " + BUILD_ERROR_RETRY_MAX_KEY);
        }
        if (retryCount < 0 || retryMax < 0) {
            return RetryState.invalid("negative retry values");
        }
        if (retryMax == 0) {
            return RetryState.invalid(BUILD_ERROR_RETRY_MAX_KEY + " must be greater than 0");
        }
        return RetryState.valid(retryCount, retryMax);
    }

    private Integer optionalInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WorkflowStepResult stopFailure(String message) {
        return new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                message,
                Map.of(
                        FINAL_DECISION_KEY, WorkflowStepDecision.STOP_FAILURE.name(),
                        NEXT_ACTION_KEY, "Inspect build retry state and rerun after fixing the workflow context",
                        CORRECTION_TRIGGERED_KEY, false
                )
        );
    }

    private WorkflowStepResult stopAtRetryLimit(WorkflowExecutionContext context,
                                                WorkflowStepResult result,
                                                RetryState retryState) {
        if (isRetryLimitWaitHumanCase(context, result, retryState)) {
            return buildPersistentBuildFailureWaitHuman(context, result);
        }
        return attachWorkflowSummarySafely(
                context,
                stopFailure("Build retry limit reached: " + retryState.retryCount() + "/" + retryState.retryMax())
        );
    }

    private WorkflowStepResult convertPersistentBuildFailureToWaitHuman(
            WorkflowExecutionContext context,
            WorkflowStepResult result) {
        if (!isPersistentBuildFailureAfterCorrection(context, result)) {
            return result;
        }

        return buildPersistentBuildFailureWaitHuman(context, result);
    }

    private WorkflowStepResult buildPersistentBuildFailureWaitHuman(WorkflowExecutionContext context,
                                                                   WorkflowStepResult result) {
        Map<String, Object> data = new HashMap<>(result.data());
        putIfPresent(data, context.variables(), BUILD_ARTIFACT_PATH_KEY);
        putIfPresent(data, context.variables(), CORRECTION_ATTEMPT_PATH_KEY);
        putIfPresent(data, context.variables(), BUILD_ERROR_RETRY_COUNT_KEY);
        putIfPresent(data, context.variables(), BUILD_ERROR_RETRY_MAX_KEY);
        log.warn("Workflow WAIT_HUMAN — persistent build failure after correction: runId={}",
                context.workflowRun().runId());
        data.put(BUILD_STATUS_KEY, BUILD_FAILURE_STATUS);
        data.put(FINAL_DECISION_KEY, WorkflowStepDecision.WAIT_HUMAN.name());
        data.put(WAIT_REASON_KEY, PERSISTENT_BUILD_FAILURE_WAIT_REASON);
        data.put(NEXT_ACTION_KEY, PERSISTENT_BUILD_FAILURE_NEXT_ACTION);
        data.put(CORRECTION_TRIGGERED_KEY, true);

        WorkflowStepResult waitHumanResult = new WorkflowStepResult(
                WorkflowStepDecision.WAIT_HUMAN,
                PERSISTENT_BUILD_FAILURE_WAIT_REASON,
                data
        );
        return attachWorkflowSummarySafely(context, waitHumanResult);
    }

    private boolean isRetryLimitWaitHumanCase(WorkflowExecutionContext context,
                                              WorkflowStepResult result,
                                              RetryState retryState) {
        if (result.decision() != WorkflowStepDecision.RETRY_CORRECTION) {
            return false;
        }
        if (retryState.retryCount() < retryState.retryMax()) {
            return false;
        }
        if (!BUILD_FAILURE_STATUS.equals(toText(result.data().get(BUILD_STATUS_KEY), ""))) {
            return false;
        }
        return isTrue(result.data().get(CORRECTION_TRIGGERED_KEY))
                || isTrue(context.variables().get(CORRECTION_TRIGGERED_KEY));
    }

    private boolean isPersistentBuildFailureAfterCorrection(WorkflowExecutionContext context,
                                                           WorkflowStepResult result) {
        if (result.decision() != WorkflowStepDecision.STOP_FAILURE) {
            return false;
        }
        Map<String, Object> data = result.data();
        if (!BUILD_FAILURE_STATUS.equals(toText(data.get(BUILD_STATUS_KEY), ""))) {
            return false;
        }
        if (!isTrue(data.get(CORRECTION_TRIGGERED_KEY)) && !isTrue(context.variables().get(CORRECTION_TRIGGERED_KEY))) {
            return false;
        }
        Integer retryCount = optionalInt(data.get(BUILD_ERROR_RETRY_COUNT_KEY));
        Integer retryMax = optionalInt(data.get(BUILD_ERROR_RETRY_MAX_KEY));
        return retryCount != null && retryMax != null && retryMax > 0 && retryCount >= retryMax;
    }

    private void putIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.putIfAbsent(key, value);
        }
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && "true".equalsIgnoreCase(value.toString().trim());
    }

    private String buildSummaryContent(WorkflowStepResult result) {
        StringBuilder summary = new StringBuilder();
        String status = result.decision() != null
                ? result.decision().name()
                : toText(result.data().get(FINAL_DECISION_KEY), "UNKNOWN");
        summary.append("Status: ").append(status).append("\n");
        summary.append("Reason: ").append(resolveReason(result)).append("\n\n");
        summary.append("Context:\n").append(summaryContext(result)).append("\n\n");
        summary.append("Actions:\n");

        List<String> actions = summaryActions(result);
        for (int i = 0; i < actions.size(); i++) {
            summary.append(i + 1).append(". ").append(actions.get(i)).append("\n");
        }

        summary.append("\nArtifacts:\n");
        List<String> artifacts = summaryArtifacts(result);
        if (artifacts.isEmpty()) {
            summary.append("- none\n");
        } else {
            for (String artifact : artifacts) {
                summary.append("- ").append(artifact).append("\n");
            }
        }
        return summary.toString();
    }

    private String resolveReason(WorkflowStepResult result) {
        if (result.decision() == WorkflowStepDecision.WAIT_HUMAN) {
            return toText(result.data().get(WAIT_REASON_KEY), "Human decision required");
        }
        if (result.decision() == WorkflowStepDecision.STOP_FAILURE) {
            return toText(result.message(), "Workflow failure");
        }
        return "Workflow step completed";
    }

    private String summaryContext(WorkflowStepResult result) {
        return switch (result.decision()) {
            case CONTINUE -> "The workflow completed successfully.";
            case STOP_FAILURE -> "A technical issue stopped the workflow.";
            case WAIT_HUMAN -> isBuildWaitHuman(result)
                    ? "Automatic correction was attempted, but the build still fails."
                    : "A human decision is needed on the review result.";
            default -> "The workflow reached an intermediate state.";
        };
    }

    private List<String> summaryActions(WorkflowStepResult result) {
        return switch (result.decision()) {
            case CONTINUE -> List.of("Continue to the next phase.");
            case STOP_FAILURE -> List.of(
                    "Inspect the artifact.",
                    "Fix the technical issue.",
                    "Rerun the workflow."
            );
            case WAIT_HUMAN -> isBuildWaitHuman(result)
                    ? List.of(
                            "Inspect the build artifact.",
                            "Inspect the correction attempt.",
                            "Fix manually or adjust strategy.",
                            "Resume the workflow."
                    )
                    : List.of(
                            "Inspect the review result.",
                            "Update the decision if needed.",
                            "Resume the workflow."
                    );
            default -> List.of("Inspect the workflow state.");
        };
    }

    private List<String> summaryArtifacts(WorkflowStepResult result) {
        List<String> artifacts = new java.util.ArrayList<>();
        if (result.decision() == WorkflowStepDecision.WAIT_HUMAN) {
            if (isBuildWaitHuman(result)) {
                addArtifact(artifacts, "Build artifact", valueOrFallback(
                        result.data().get(BUILD_RESULT_PATH_KEY),
                        result.data().get(BUILD_ARTIFACT_PATH_KEY)
                ));
                addArtifact(artifacts, "Correction attempt", result.data().get(CORRECTION_ATTEMPT_PATH_KEY));
            } else {
                addArtifact(artifacts, "Review result", valueOrFallback(
                        result.data().get(REVIEW_RESULT_PATH_KEY),
                        result.data().get(RESULT_ARTIFACT_PATH_KEY)
                ));
            }
        } else if (result.decision() == WorkflowStepDecision.STOP_FAILURE) {
            addArtifact(artifacts, "Build artifact", valueOrFallback(
                    result.data().get(BUILD_RESULT_PATH_KEY),
                    result.data().get(BUILD_ARTIFACT_PATH_KEY)
            ));
            addArtifact(artifacts, "Review result", valueOrFallback(
                    result.data().get(REVIEW_RESULT_PATH_KEY),
                    result.data().get(RESULT_ARTIFACT_PATH_KEY)
            ));
            addArtifact(artifacts, "Correction attempt", result.data().get(CORRECTION_ATTEMPT_PATH_KEY));
        } else {
            addArtifact(artifacts, "Build artifact", valueOrFallback(
                    result.data().get(BUILD_RESULT_PATH_KEY),
                    result.data().get(BUILD_ARTIFACT_PATH_KEY)
            ));
            addArtifact(artifacts, "Result artifact", result.data().get(RESULT_ARTIFACT_PATH_KEY));
        }

        addArtifact(artifacts, "Summary", result.data().get(WORKFLOW_SUMMARY_PATH_KEY));
        return artifacts;
    }

    private void addArtifact(List<String> artifacts, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return;
        }
        artifacts.add(label + ": " + text);
    }

    private Object valueOrFallback(Object primary, Object fallback) {
        return primary != null ? primary : fallback;
    }

    private boolean isBuildWaitHuman(WorkflowStepResult result) {
        return BUILD_FAILURE_STATUS.equals(toText(result.data().get(BUILD_STATUS_KEY), ""))
                || PERSISTENT_BUILD_FAILURE_WAIT_REASON.equals(toText(result.data().get(WAIT_REASON_KEY), ""));
    }

    private String defaultNextAction(WorkflowStepDecision decision) {
        return switch (decision) {
            case WAIT_HUMAN -> "Edit review result and resume with runCorrectionAfterReview(...)";
            case STOP_FAILURE -> "Inspect failure and related artifacts, then rerun";
            case CONTINUE -> "Continue workflow execution";
            default -> "Inspect workflow state";
        };
    }

    private String requiredString(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required variable: " + key);
        }
        return value.toString().trim();
    }

    private int requiredInt(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required variable: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString().trim());
    }

    private Path requiredPath(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required variable: " + key);
        }
        if (value instanceof Path path) {
            return path;
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Invalid variable: " + key);
        }
        return Path.of(text);
    }

    private String toText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isBlank() ? fallback : text;
    }

    private boolean isPresent(Object value) {
        return value != null && !value.toString().isBlank();
    }

    private record RetryState(Integer retryCount, Integer retryMax, String errorMessage) {

        private static RetryState valid(int retryCount, int retryMax) {
            return new RetryState(retryCount, retryMax, null);
        }

        private static RetryState invalid(String errorMessage) {
            return new RetryState(null, null, errorMessage);
        }

        private boolean valid() {
            return errorMessage == null;
        }
    }
}
