package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionException;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionRequest;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Requests a Codex correction after a Maven build failure.
 *
 * <p>The step consumes a short build error summary and the build result artifact path from the
 * workflow context. It does not parse Maven output beyond the data prepared by
 * {@link MavenValidationStep}.
 */
public class BuildErrorCorrectionStep implements WorkflowStep {

    private static final String ERROR_PREFIX = "BuildErrorCorrectionStep: ";
    private static final String COMPLETED_MESSAGE = "Build error analysis and correction attempted";
    private static final String CONTINUE_NEXT_ACTION = "Build error correction attempted. Rerun Maven validation";
    private static final String STOP_FAILURE_NEXT_ACTION =
            "Inspect build error correction failure and rerun after fixing the issue";

    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_BUILD_ERROR_SUMMARY = "buildErrorSummary";
    private static final String VAR_BUILD_ARTIFACT_PATH = "buildArtifactPath";
    private static final String VAR_BUILD_ERROR_RETRY_COUNT = "buildErrorRetryCount";
    private static final String VAR_BUILD_ERROR_RETRY_MAX = "buildErrorRetryMax";
    private static final String VAR_CODEX_WORKING_DIRECTORY = "codexWorkingDirectory";
    private static final String VAR_CODEX_TIMEOUT_SECONDS = "codexTimeoutSeconds";

    private final CodexWorkflowClient codexWorkflowClient;
    private final WorkflowArtifactService artifactService;

    public BuildErrorCorrectionStep(CodexWorkflowClient codexWorkflowClient,
                                    WorkflowArtifactService artifactService) {
        this.codexWorkflowClient = Objects.requireNonNull(codexWorkflowClient, "codexWorkflowClient must not be null");
        this.artifactService = Objects.requireNonNull(artifactService, "artifactService must not be null");
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
            String buildErrorSummary = requiredString(context, VAR_BUILD_ERROR_SUMMARY);
            String buildArtifactPath = requiredString(context, VAR_BUILD_ARTIFACT_PATH);
            Integer retryCount = optionalInt(context, VAR_BUILD_ERROR_RETRY_COUNT);
            Integer retryMax = optionalInt(context, VAR_BUILD_ERROR_RETRY_MAX);

            CodexExecutionRequest request = new CodexExecutionRequest(
                    buildPrompt(buildErrorSummary, buildArtifactPath),
                    optionalPath(context, VAR_CODEX_WORKING_DIRECTORY),
                    optionalInt(context, VAR_CODEX_TIMEOUT_SECONDS)
            );

            CodexExecutionResult codexResult = codexWorkflowClient.execute(request);
            Path resultPath = artifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.BUILD_ERROR_CORRECTION
            );
            artifactService.writeArtifact(resultPath, buildResultContent(codexResult, retryCount, retryMax));

            if (!codexResult.success()) {
                return stopFailure(codexResult.timedOut()
                        ? "Codex execution timed out"
                        : "Codex execution was not successful");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("resultArtifactPath", resultPath.toString());
            data.put("correctionAttemptPath", resultPath.toString());
            data.put("buildArtifactPath", buildArtifactPath);
            data.put("finalDecision", WorkflowStepDecision.CONTINUE.name());
            data.put("nextAction", CONTINUE_NEXT_ACTION);
            data.put("correctionTriggered", true);
            if (retryCount != null) {
                data.put(VAR_BUILD_ERROR_RETRY_COUNT, retryCount);
            }
            if (retryMax != null) {
                data.put(VAR_BUILD_ERROR_RETRY_MAX, retryMax);
            }

            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    COMPLETED_MESSAGE,
                    data
            );
        } catch (IllegalArgumentException | CodexExecutionException | IllegalStateException e) {
            return stopFailure(e.getMessage());
        }
    }

    private String buildPrompt(String buildErrorSummary, String buildArtifactPath) {
        return "Build Compilation Error Analysis and Correction\n\n"
                + "[Build Artifact]\n"
                + buildArtifactPath
                + "\n\n"
                + "[Build Error]\n"
                + buildErrorSummary
                + "\n\n"
                + "[Task]\n"
                + "Analyze the compilation error above and provide corrected code.\n"
                + "Fix the specific issue mentioned in the error.\n"
                + "Ensure the corrected code compiles cleanly.";
    }

    private String buildResultContent(CodexExecutionResult result, Integer retryCount, Integer retryMax) {
        String attemptLine = "Attempt: " + valueOrUnknown(retryCount) + " / " + valueOrUnknown(retryMax) + "\n\n";
        if (result.stderr().isBlank()) {
            return attemptLine + result.stdout();
        }
        return attemptLine + result.stdout() + "\n\n[stderr]\n" + result.stderr();
    }

    private String valueOrUnknown(Integer value) {
        return value == null ? "unknown" : value.toString();
    }

    private WorkflowStepResult stopFailure(String message) {
        String cause = (message == null || message.isBlank()) ? "unexpected failure" : message;
        return new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                ERROR_PREFIX + cause,
                Map.of(
                        "finalDecision", WorkflowStepDecision.STOP_FAILURE.name(),
                        "nextAction", STOP_FAILURE_NEXT_ACTION,
                        "correctionTriggered", false
                )
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
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid variable: " + key, e);
        }
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
