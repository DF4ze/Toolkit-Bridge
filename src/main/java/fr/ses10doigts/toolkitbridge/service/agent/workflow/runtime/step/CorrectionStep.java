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

public class CorrectionStep implements WorkflowStep {

    private static final String ERROR_PREFIX = "CorrectionStep: ";
    private static final String COMPLETED_MESSAGE = "Correction completed";
    private static final String CONTINUE_NEXT_ACTION = "Correction done. Continue workflow validation or next step";
    private static final String STOP_FAILURE_NEXT_ACTION = "Inspect correction failure details and related artifacts, then rerun correction";
    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_CODEX_WORKING_DIRECTORY = "codexWorkingDirectory";
    private static final String VAR_CODEX_TIMEOUT_SECONDS = "codexTimeoutSeconds";

    private final CodexWorkflowClient codexWorkflowClient;
    private final WorkflowArtifactService workflowArtifactService;

    public CorrectionStep(CodexWorkflowClient codexWorkflowClient,
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

            Path analysisResultPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.ANALYSIS_RESULT
            );
            Path reviewResultPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.REVIEW_RESULT
            );

            if (!workflowArtifactService.artifactExists(analysisResultPath)) {
                return stopFailure("Missing required artifact: " + analysisResultPath);
            }
            if (!workflowArtifactService.artifactExists(reviewResultPath)) {
                return stopFailure("Missing required artifact: " + reviewResultPath);
            }

            String analysisResultContent = workflowArtifactService.readArtifact(analysisResultPath);
            String reviewResultContent = workflowArtifactService.readArtifact(reviewResultPath);
            String correctionPrompt = buildPrompt(context, analysisResultPath, analysisResultContent, reviewResultPath, reviewResultContent);

            Path correctionPromptPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.CORRECTION_PROMPT
            );
            workflowArtifactService.writeArtifact(correctionPromptPath, correctionPrompt);

            CodexExecutionRequest request = new CodexExecutionRequest(
                    correctionPrompt,
                    optionalPath(context, VAR_CODEX_WORKING_DIRECTORY),
                    optionalInt(context, VAR_CODEX_TIMEOUT_SECONDS)
            );

            CodexExecutionResult codexResult = codexWorkflowClient.execute(request);
            String correctionResultContent = buildResultContent(codexResult);

            Path correctionResultPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.CORRECTION_RESULT
            );
            workflowArtifactService.writeArtifact(correctionResultPath, correctionResultContent);

            if (!codexResult.success()) {
                String failureMessage = codexResult.timedOut()
                        ? "Codex execution timed out"
                        : "Codex execution was not successful";
                return new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        ERROR_PREFIX + failureMessage,
                        failureData(correctionPromptPath, correctionResultPath)
                );
            }

            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    COMPLETED_MESSAGE,
                    Map.of(
                            "promptArtifactPath", correctionPromptPath.toString(),
                            "resultArtifactPath", correctionResultPath.toString(),
                            "correctionPromptPath", correctionPromptPath.toString(),
                            "correctionResultPath", correctionResultPath.toString(),
                            "finalDecision", WorkflowStepDecision.CONTINUE.name(),
                            "correctionTriggered", true,
                            "nextAction", CONTINUE_NEXT_ACTION
                    )
            );
        } catch (IllegalArgumentException | CodexExecutionException | IllegalStateException e) {
            return stopFailure(e.getMessage());
        }
    }

    private String buildPrompt(WorkflowExecutionContext context,
                               Path analysisResultPath,
                               String analysisResultContent,
                               Path reviewResultPath,
                               String reviewResultContent) {
        return "Correction request\n"
                + "runId: " + context.workflowRun().runId() + "\n"
                + "workflowType: " + context.workflowRun().workflowType() + "\n"
                + "targetStepRef: " + context.workflowRun().targetStepRef() + "\n"
                + "analysisResultPath: " + analysisResultPath + "\n"
                + "reviewResultPath: " + reviewResultPath + "\n\n"
                + "Analysis result\n"
                + analysisResultContent
                + "\n\nReview result\n"
                + reviewResultContent
                + "\n\nProvide a corrected version based on the review";
    }

    private String buildResultContent(CodexExecutionResult result) {
        if (result.stderr().isBlank()) {
            return result.stdout();
        }
        return result.stdout() + "\n\n[stderr]\n" + result.stderr();
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

    private Map<String, Object> failureData(Path correctionPromptPath, Path correctionResultPath) {
        return Map.of(
                "promptArtifactPath", correctionPromptPath.toString(),
                "resultArtifactPath", correctionResultPath.toString(),
                "correctionPromptPath", correctionPromptPath.toString(),
                "correctionResultPath", correctionResultPath.toString(),
                "finalDecision", WorkflowStepDecision.STOP_FAILURE.name(),
                "correctionTriggered", true,
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
