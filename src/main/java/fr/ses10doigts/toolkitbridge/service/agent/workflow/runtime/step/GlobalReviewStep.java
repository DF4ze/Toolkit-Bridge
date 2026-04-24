package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionException;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionRequest;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class GlobalReviewStep implements WorkflowStep {

    private static final String ERROR_PREFIX = "GlobalReviewStep: ";
    private static final String COMPLETED_MESSAGE = "Global review completed";
    private static final String WAIT_HUMAN_MESSAGE = "Global review requires human decision";
    private static final String WAIT_HUMAN_NEXT_ACTION = "Review result requires human decision. Edit the review result, add a clear decision, then run runCorrectionAfterReview(...)";
    private static final String CONTINUE_NEXT_ACTION = "Review completed. Continue workflow execution";
    private static final String STOP_FAILURE_NEXT_ACTION = "Inspect review failure details and related artifacts, then rerun the workflow";
    private static final String NEED_CORRECTION_MARKER = "DECISION: NEED_CORRECTION";
    private static final String OK_MARKER = "DECISION: OK";
    private static final String WAIT_HUMAN_MARKER = "DECISION: WAIT_HUMAN";
    private static final String WAIT_REASON_PREFIX = "WAIT_REASON:";
    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_CODEX_WORKING_DIRECTORY = "codexWorkingDirectory";
    private static final String VAR_CODEX_TIMEOUT_SECONDS = "codexTimeoutSeconds";

    private final CodexWorkflowClient codexWorkflowClient;
    private final WorkflowArtifactService workflowArtifactService;

    public GlobalReviewStep(CodexWorkflowClient codexWorkflowClient,
                            WorkflowArtifactService workflowArtifactService) {
        this.codexWorkflowClient = Objects.requireNonNull(codexWorkflowClient, "codexWorkflowClient must not be null");
        this.workflowArtifactService = Objects.requireNonNull(workflowArtifactService, "workflowArtifactService must not be null");
    }

    @Override
    public WorkflowStepResult execute(WorkflowExecutionContext context) {
        if (context == null) {
            return stopFailure("context must not be null");
        }

        try {
            Path reportRootDirectory = requiredPath(context, VAR_REPORT_ROOT_DIRECTORY);
            String reportVersion = requiredString(context, VAR_REPORT_VERSION);
            String reportPhase = requiredString(context, VAR_REPORT_PHASE);
            int stepNumber = requiredInt(context, VAR_STEP_NUMBER);

            Path analysisPromptPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.ANALYSIS_PROMPT
            );
            Path analysisResultPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.ANALYSIS_RESULT
            );

            if (!workflowArtifactService.artifactExists(analysisPromptPath)) {
                return stopFailure("Missing required artifact: " + analysisPromptPath);
            }
            if (!workflowArtifactService.artifactExists(analysisResultPath)) {
                return stopFailure("Missing required artifact: " + analysisResultPath);
            }

            String analysisPromptContent = workflowArtifactService.readArtifact(analysisPromptPath);
            String analysisResultContent = workflowArtifactService.readArtifact(analysisResultPath);
            String reviewPrompt = buildPrompt(context, analysisPromptPath, analysisPromptContent, analysisResultPath, analysisResultContent);

            Path reviewPromptPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.REVIEW_PROMPT
            );
            workflowArtifactService.writeArtifact(reviewPromptPath, reviewPrompt);

            CodexExecutionRequest request = new CodexExecutionRequest(
                    reviewPrompt,
                    optionalPath(context, VAR_CODEX_WORKING_DIRECTORY),
                    optionalInt(context, VAR_CODEX_TIMEOUT_SECONDS)
            );

            CodexExecutionResult codexResult = codexWorkflowClient.execute(request);
            String semanticReviewOutput = codexResult.stdout();
            String reviewResultContent = buildResultContent(codexResult);

            Path reviewResultPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.REVIEW_RESULT
            );
            workflowArtifactService.writeArtifact(reviewResultPath, reviewResultContent);

            if (!codexResult.success()) {
                String failureMessage = codexResult.timedOut()
                        ? "Codex execution timed out"
                        : "Codex execution was not successful";
                return new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        ERROR_PREFIX + failureMessage,
                        failureData(reviewPromptPath, reviewResultPath)
                );
            }

            ReviewDirective reviewDirective = parseReviewDirective(semanticReviewOutput);
            if (reviewDirective == ReviewDirective.WAIT_HUMAN) {
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("promptArtifactPath", reviewPromptPath.toString());
                data.put("resultArtifactPath", reviewResultPath.toString());
                data.put("reviewPromptPath", reviewPromptPath.toString());
                data.put("reviewResultPath", reviewResultPath.toString());
                data.put("finalDecision", WorkflowStepDecision.WAIT_HUMAN.name());
                data.put("correctionTriggered", false);
                data.put("nextAction", WAIT_HUMAN_NEXT_ACTION);

                String waitReason = extractWaitReason(semanticReviewOutput);
                if (waitReason != null) {
                    data.put("waitReason", waitReason);
                }

                return new WorkflowStepResult(
                        WorkflowStepDecision.WAIT_HUMAN,
                        WAIT_HUMAN_MESSAGE,
                        data
                );
            }

            boolean requiresCorrection = reviewDirective == ReviewDirective.NEED_CORRECTION;
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("promptArtifactPath", reviewPromptPath.toString());
            data.put("resultArtifactPath", reviewResultPath.toString());
            data.put("reviewPromptPath", reviewPromptPath.toString());
            data.put("reviewResultPath", reviewResultPath.toString());
            data.put("requiresCorrection", requiresCorrection);
            data.put("finalDecision", WorkflowStepDecision.CONTINUE.name());
            data.put("correctionTriggered", false);
            data.put("nextAction", CONTINUE_NEXT_ACTION);

            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    COMPLETED_MESSAGE,
                    data
            );
        } catch (IllegalArgumentException | CodexExecutionException | IllegalStateException e) {
            return stopFailure(e.getMessage());
        }
    }

    private String buildPrompt(WorkflowExecutionContext context,
                               Path analysisPromptPath,
                               String analysisPromptContent,
                               Path analysisResultPath,
                               String analysisResultContent) {
        return "Global review request\n"
                + "runId: " + context.workflowRun().runId() + "\n"
                + "workflowType: " + context.workflowRun().workflowType() + "\n"
                + "targetStepRef: " + context.workflowRun().targetStepRef() + "\n"
                + "analysisPromptPath: " + analysisPromptPath + "\n"
                + "analysisResultPath: " + analysisResultPath + "\n\n"
                + "[analysis_prompt]\n"
                + analysisPromptContent
                + "\n\n[analysis_result]\n"
                + analysisResultContent;
    }

    private String buildResultContent(CodexExecutionResult result) {
        if (result.stderr().isBlank()) {
            return result.stdout();
        }
        return result.stdout() + "\n\n[stderr]\n" + result.stderr();
    }

    private ReviewDirective parseReviewDirective(String reviewResultContent) {
        if (reviewResultContent == null || reviewResultContent.isBlank()) {
            return ReviewDirective.NONE;
        }

        boolean hasWaitHuman = false;
        boolean hasNeedCorrection = false;
        boolean hasOk = false;

        for (String rawLine : reviewResultContent.split("\\R")) {
            String line = rawLine.trim();
            if (line.equals(WAIT_HUMAN_MARKER)) {
                hasWaitHuman = true;
            }
            if (line.equals(NEED_CORRECTION_MARKER)) {
                hasNeedCorrection = true;
            }
            if (line.equals(OK_MARKER)) {
                hasOk = true;
            }
        }

        if (hasWaitHuman) {
            return ReviewDirective.WAIT_HUMAN;
        }
        if (hasNeedCorrection) {
            return ReviewDirective.NEED_CORRECTION;
        }
        if (hasOk) {
            return ReviewDirective.OK;
        }
        return ReviewDirective.NONE;
    }

    private String extractWaitReason(String reviewResultContent) {
        if (reviewResultContent == null || reviewResultContent.isBlank()) {
            return null;
        }
        for (String rawLine : reviewResultContent.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith(WAIT_REASON_PREFIX)) {
                String reason = line.substring(WAIT_REASON_PREFIX.length()).trim();
                if (!reason.isBlank()) {
                    return reason;
                }
            }
        }
        return null;
    }

    private enum ReviewDirective {
        WAIT_HUMAN,
        NEED_CORRECTION,
        OK,
        NONE
    }

    private WorkflowStepResult stopFailure(String message) {
        String cause = (message == null || message.isBlank()) ? "unexpected failure" : message;
        return new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                ERROR_PREFIX + cause,
                Map.of(
                        "finalDecision", WorkflowStepDecision.STOP_FAILURE.name(),
                        "correctionTriggered", false,
                        "nextAction", STOP_FAILURE_NEXT_ACTION
                )
        );
    }

    private Map<String, Object> failureData(Path reviewPromptPath, Path reviewResultPath) {
        return Map.of(
                "promptArtifactPath", reviewPromptPath.toString(),
                "resultArtifactPath", reviewResultPath.toString(),
                "reviewPromptPath", reviewPromptPath.toString(),
                "reviewResultPath", reviewResultPath.toString(),
                "finalDecision", WorkflowStepDecision.STOP_FAILURE.name(),
                "correctionTriggered", false,
                "nextAction", STOP_FAILURE_NEXT_ACTION
        );
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
            int parsed = number.intValue();
            if (parsed <= 0) {
                throw new IllegalArgumentException("Invalid variable: " + key);
            }
            return parsed;
        }
        try {
            int parsed = Integer.parseInt(value.toString().trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException("Invalid variable: " + key);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid variable: " + key, e);
        }
    }

    private Path requiredPath(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required variable: " + key);
        }
        return toPath(value, key);
    }

    private Path optionalPath(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null) {
            return null;
        }
        return toPath(value, key);
    }

    private Integer optionalInt(WorkflowExecutionContext context, String key) {
        Object value = context.variables().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid variable: " + key, e);
        }
    }

    private Path toPath(Object value, String key) {
        if (value instanceof Path path) {
            return path;
        }
        String text = value.toString();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Invalid variable: " + key);
        }
        return Path.of(text.trim());
    }
}
