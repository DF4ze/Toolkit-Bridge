package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final WorkflowOrchestrator workflowOrchestrator;
    private final WorkflowArtifactService workflowArtifactService;
    private final WorkflowStep analysisStep;
    private final WorkflowStep reviewStep;
    private final WorkflowStep correctionStep;

    public AnalysisReviewWorkflowRunner(WorkflowOrchestrator workflowOrchestrator,
                                        WorkflowArtifactService workflowArtifactService,
                                        WorkflowStep analysisStep,
                                        WorkflowStep reviewStep,
                                        WorkflowStep correctionStep) {
        this.workflowOrchestrator = Objects.requireNonNull(workflowOrchestrator, "workflowOrchestrator must not be null");
        this.workflowArtifactService = Objects.requireNonNull(workflowArtifactService, "workflowArtifactService must not be null");
        this.analysisStep = Objects.requireNonNull(analysisStep, "analysisStep must not be null");
        this.reviewStep = Objects.requireNonNull(reviewStep, "reviewStep must not be null");
        this.correctionStep = Objects.requireNonNull(correctionStep, "correctionStep must not be null");
    }

    public WorkflowStepResult runAnalysisReviewWithOptionalCorrection(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        WorkflowStepResult result = workflowOrchestrator.executeAnalysisReviewWithOptionalCorrection(
                context,
                analysisStep,
                reviewStep,
                correctionStep
        );
        return attachWorkflowSummarySafely(context, result);
    }

    public WorkflowStepResult runCorrectionAfterReview(WorkflowExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");
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

        String summaryContent = buildSummaryContent(result);
        workflowArtifactService.writeArtifact(summaryPath, summaryContent);

        Map<String, Object> data = new HashMap<>(result.data());
        data.put(WORKFLOW_SUMMARY_PATH_KEY, summaryPath.toString());
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

    private String buildSummaryContent(WorkflowStepResult result) {
        String finalDecision = toText(result.data().get(FINAL_DECISION_KEY), result.decision().name());
        String reason = resolveReason(result);
        String correctionTriggered = toText(result.data().get(CORRECTION_TRIGGERED_KEY), "false");
        String nextAction = toText(result.data().get(NEXT_ACTION_KEY), defaultNextAction(result.decision()));

        String artifactLines = result.data().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getKey().endsWith("Path"))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        if (artifactLines.isBlank()) {
            artifactLines = "- none";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Decision: ").append(finalDecision).append("\n");
        summary.append("Reason: ").append(reason).append("\n");
        summary.append("Correction triggered: ").append(correctionTriggered).append("\n\n");
        summary.append("Artifacts:\n").append(artifactLines).append("\n\n");
        summary.append("Next steps:\n");

        if (result.decision() == WorkflowStepDecision.WAIT_HUMAN) {
            String reviewPath = toText(result.data().get("reviewResultPath"), toText(result.data().get("resultArtifactPath"), "review result artifact"));
            summary.append("1. Edit review result: ").append(reviewPath).append("\n");
            summary.append("2. Add a clear decision and optional WAIT_REASON update\n");
            summary.append("3. Resume with runCorrectionAfterReview(...)\n");
        } else {
            summary.append("1. ").append(nextAction).append("\n");
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
}
