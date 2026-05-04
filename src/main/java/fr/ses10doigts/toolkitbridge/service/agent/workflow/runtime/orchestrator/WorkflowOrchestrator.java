package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class WorkflowOrchestrator {
    private static final String NO_CORRECTION_REQUIRED_MESSAGE = "Review completed - no correction required";
    private static final String REQUIRES_CORRECTION_KEY = "requiresCorrection";
    private static final String FINAL_DECISION_KEY = "finalDecision";
    private static final String CORRECTION_TRIGGERED_KEY = "correctionTriggered";
    private static final String NEXT_ACTION_KEY = "nextAction";

    public WorkflowStepResult executeSingleStep(WorkflowExecutionContext context, WorkflowStep step) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(step, "step must not be null");

        String runId = context.workflowRun().runId();
        log.debug("Executing workflow step: runId={}, step={}", runId, step.getClass().getSimpleName());

        WorkflowStepResult result = step.execute(context);
        if (result == null) {
            throw new IllegalStateException("step result must not be null");
        }

        log.debug("Workflow step completed: runId={}, step={}, decision={}",
                runId, step.getClass().getSimpleName(), result.decision());

        WorkflowStepDecision decision = result.decision();
        return switch (decision) {
            case CONTINUE, WAIT_HUMAN, STOP_FAILURE -> result;
            default -> throw new IllegalStateException("Unsupported workflow decision at this stage: " + decision);
        };
    }

    public WorkflowStepResult executeTwoSteps(WorkflowExecutionContext context,
                                              WorkflowStep firstStep,
                                              WorkflowStep secondStep) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(firstStep, "firstStep must not be null");
        Objects.requireNonNull(secondStep, "secondStep must not be null");

        WorkflowStepResult firstResult = executeSingleStep(context, firstStep);
        if (firstResult.decision() != WorkflowStepDecision.CONTINUE) {
            return firstResult;
        }

        return executeSingleStep(context, secondStep);
    }

    public WorkflowStepResult executeThreeSteps(WorkflowExecutionContext context,
                                                WorkflowStep firstStep,
                                                WorkflowStep secondStep,
                                                WorkflowStep thirdStep) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(firstStep, "firstStep must not be null");
        Objects.requireNonNull(secondStep, "secondStep must not be null");
        Objects.requireNonNull(thirdStep, "thirdStep must not be null");

        WorkflowStepResult firstResult = executeSingleStep(context, firstStep);
        if (firstResult.decision() != WorkflowStepDecision.CONTINUE) {
            return firstResult;
        }

        WorkflowStepResult secondResult = executeSingleStep(context, secondStep);
        if (secondResult.decision() != WorkflowStepDecision.CONTINUE) {
            return secondResult;
        }

        return executeSingleStep(context, thirdStep);
    }

    public WorkflowStepResult executeAnalysisReviewWithOptionalCorrection(WorkflowExecutionContext context,
                                                                          WorkflowStep analysisStep,
                                                                          WorkflowStep reviewStep,
                                                                          WorkflowStep correctionStep) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(analysisStep, "analysisStep must not be null");
        Objects.requireNonNull(reviewStep, "reviewStep must not be null");
        Objects.requireNonNull(correctionStep, "correctionStep must not be null");

        WorkflowStepResult analysisResult = executeSingleStep(context, analysisStep);
        if (analysisResult.decision() != WorkflowStepDecision.CONTINUE) {
            return enrichObservability(analysisResult, false);
        }

        WorkflowStepResult reviewResult = executeSingleStep(context, reviewStep);
        if (reviewResult.decision() != WorkflowStepDecision.CONTINUE) {
            return enrichObservability(reviewResult, false);
        }

        if (requiresCorrection(reviewResult)) {
            log.debug("Correction triggered by review: runId={}", context.workflowRun().runId());
            WorkflowStepResult correctionResult = executeSingleStep(context, correctionStep);
            return enrichObservability(correctionResult, true);
        }

        Map<String, Object> resultData = new HashMap<>(reviewResult.data());
        resultData.put(REQUIRES_CORRECTION_KEY, false);
        resultData.put(FINAL_DECISION_KEY, WorkflowStepDecision.CONTINUE.name());
        resultData.put(CORRECTION_TRIGGERED_KEY, false);
        resultData.putIfAbsent(NEXT_ACTION_KEY, "No correction required. Workflow can continue");
        return new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                NO_CORRECTION_REQUIRED_MESSAGE,
                resultData
        );
    }

    public WorkflowStepResult executeAnalysisReviewWithCorrectionAndValidation(
            WorkflowExecutionContext context,
            WorkflowStep analysisStep,
            WorkflowStep reviewStep,
            WorkflowStep correctionStep,
            WorkflowStep validationStep) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(analysisStep, "analysisStep must not be null");
        Objects.requireNonNull(reviewStep, "reviewStep must not be null");
        Objects.requireNonNull(correctionStep, "correctionStep must not be null");
        Objects.requireNonNull(validationStep, "validationStep must not be null");

        WorkflowStepResult analysisResult = executeSingleStep(context, analysisStep);
        if (analysisResult.decision() != WorkflowStepDecision.CONTINUE) {
            return enrichObservability(analysisResult, false);
        }

        WorkflowStepResult reviewResult = executeSingleStep(context, reviewStep);
        if (reviewResult.decision() != WorkflowStepDecision.CONTINUE) {
            return enrichObservability(reviewResult, false);
        }

        boolean correctionTriggered = false;
        if (requiresCorrection(reviewResult)) {
            log.debug("Correction triggered by review: runId={}", context.workflowRun().runId());
            WorkflowStepResult correctionResult = executeSingleStep(context, correctionStep);
            if (correctionResult.decision() != WorkflowStepDecision.CONTINUE) {
                return enrichObservability(correctionResult, true);
            }
            correctionTriggered = true;
        }

        WorkflowStepResult validationResult = executeSingleStep(context, validationStep);
        return enrichObservability(validationResult, correctionTriggered);
    }

    private boolean requiresCorrection(WorkflowStepResult reviewResult) {
        Object value = reviewResult.data().get(REQUIRES_CORRECTION_KEY);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        throw new IllegalStateException(
                "Invalid review result contract: '" + REQUIRES_CORRECTION_KEY
                        + "' must be a boolean when decision is CONTINUE"
        );
    }

    private WorkflowStepResult enrichObservability(WorkflowStepResult result, boolean correctionTriggered) {
        Map<String, Object> data = new HashMap<>(result.data());
        data.put(FINAL_DECISION_KEY, result.decision().name());
        data.put(CORRECTION_TRIGGERED_KEY, correctionTriggered);
        data.putIfAbsent(NEXT_ACTION_KEY, defaultNextAction(result.decision(), correctionTriggered));
        return new WorkflowStepResult(result.decision(), result.message(), data);
    }

    private String defaultNextAction(WorkflowStepDecision decision, boolean correctionTriggered) {
        return switch (decision) {
            case WAIT_HUMAN -> "Edit review result with a clear decision, then run runCorrectionAfterReview(...)";
            case STOP_FAILURE -> correctionTriggered
                    ? "Inspect correction failure details and rerun correction"
                    : "Inspect failure details and rerun analysis/review workflow";
            case CONTINUE -> correctionTriggered
                    ? "Correction completed. Continue with validation"
                    : "Continue workflow execution";
            default -> "Inspect workflow state";
        };
    }
}
