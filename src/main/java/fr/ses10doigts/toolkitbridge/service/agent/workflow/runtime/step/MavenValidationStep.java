package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationCommand;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.WorkflowValidationService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MavenValidationStep implements WorkflowStep {

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_RETRY_MAX = 2;
    private static final String ERROR_PREFIX = "MavenValidationStep: ";
    private static final String COMPLETED_MESSAGE = "Maven build validation passed";
    private static final String CONTINUE_NEXT_ACTION = "Build compiles. Continue workflow execution";
    private static final String RETRY_NEXT_ACTION = "Build failed. Attempting automatic correction...";
    private static final String STOP_FAILURE_NEXT_ACTION =
            "Inspect build failure in the build result artifact, fix compilation errors, then rerun";

    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_VALIDATION_WORKING_DIRECTORY = "validationWorkingDirectory";
    private static final String VAR_VALIDATION_TIMEOUT_SECONDS = "validationTimeoutSeconds";
    private static final String VAR_BUILD_ERROR_RETRY_COUNT = "buildErrorRetryCount";
    private static final String VAR_BUILD_ERROR_RETRY_MAX = "buildErrorRetryMax";
    private static final String BUILD_ERROR_SUMMARY_KEY = "buildErrorSummary";
    private static final String BUILD_ARTIFACT_PATH_KEY = "buildArtifactPath";

    private final WorkflowValidationService validationService;
    private final WorkflowArtifactService artifactService;

    public MavenValidationStep(WorkflowValidationService validationService,
                               WorkflowArtifactService artifactService) {
        this.validationService = Objects.requireNonNull(validationService, "validationService must not be null");
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
            Path validationWorkingDirectory = requiredPath(context, VAR_VALIDATION_WORKING_DIRECTORY);
            int timeoutSeconds = optionalInt(context, VAR_VALIDATION_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);

            ValidationCommand command = new ValidationCommand(
                    buildMavenCommand(),
                    validationWorkingDirectory,
                    timeoutSeconds
            );

            ValidationResult buildResult = validationService.validate(command);

            String artifactContent = buildArtifactContent(buildResult);
            Path buildResultPath = artifactService.buildArtifactPath(
                    reportRootDirectory, reportVersion, reportPhase, stepNumber, WorkflowArtifactType.BUILD_RESULT);
            artifactService.writeArtifact(buildResultPath, artifactContent);

            if (buildResult.status() == ValidationStatus.SUCCESS) {
                return new WorkflowStepResult(
                        WorkflowStepDecision.CONTINUE,
                        COMPLETED_MESSAGE,
                        buildData(buildResult, buildResultPath, WorkflowStepDecision.CONTINUE)
                );
            }

            // Handle build failure: check if we should retry
            int retryCount = optionalInt(context, VAR_BUILD_ERROR_RETRY_COUNT, 0);
            int retryMax = optionalInt(context, VAR_BUILD_ERROR_RETRY_MAX, DEFAULT_RETRY_MAX);

            if (buildResult.status() == ValidationStatus.FAILURE && retryCount < retryMax) {
                Map<String, Object> data = buildData(buildResult, buildResultPath, WorkflowStepDecision.RETRY_CORRECTION);
                data.put(VAR_BUILD_ERROR_RETRY_COUNT, retryCount + 1);
                data.put(VAR_BUILD_ERROR_RETRY_MAX, retryMax);
                data.put(BUILD_ERROR_SUMMARY_KEY, extractErrorSummary(buildResult));
                data.put(BUILD_ARTIFACT_PATH_KEY, buildResultPath.toString());
                return new WorkflowStepResult(
                        WorkflowStepDecision.RETRY_CORRECTION,
                        "Build failed. Retry count: " + (retryCount + 1) + "/" + retryMax,
                        data
                );
            }

            String reason = buildResult.errorMessage() != null
                    ? buildResult.errorMessage()
                    : "Build exited with status " + buildResult.status().name();
            return new WorkflowStepResult(
                    WorkflowStepDecision.STOP_FAILURE,
                    ERROR_PREFIX + reason,
                    buildData(buildResult, buildResultPath, WorkflowStepDecision.STOP_FAILURE)
            );

        } catch (IllegalArgumentException e) {
            return stopFailure(e.getMessage());
        }
    }

    private List<String> buildMavenCommand() {
        if (isWindows()) {
            return List.of("cmd.exe", "/c", "mvnw.cmd", "clean", "compile", "-DskipTests");
        }
        return List.of("./mvnw", "clean", "compile", "-DskipTests");
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private Map<String, Object> buildData(ValidationResult result, Path buildResultPath,
                                          WorkflowStepDecision decision) {
        Map<String, Object> data = new HashMap<>();
        data.put("buildStatus", result.status().name());
        if (result.exitCode() != null) {
            data.put("buildExitCode", result.exitCode());
        }
        data.put("buildDurationMs", result.durationMs());
        data.put("buildResultPath", buildResultPath.toString());
        data.put("finalDecision", decision.name());
        data.put("nextAction", nextAction(decision));
        return data;
    }

    private String nextAction(WorkflowStepDecision decision) {
        return switch (decision) {
            case CONTINUE -> CONTINUE_NEXT_ACTION;
            case RETRY_CORRECTION -> RETRY_NEXT_ACTION;
            default -> STOP_FAILURE_NEXT_ACTION;
        };
    }

    private String extractErrorSummary(ValidationResult result) {
        String stderr = result.stderr();
        if (stderr.isBlank()) {
            return "Build failure (no stderr). Exit code: " + result.exitCode();
        }
        String[] lines = stderr.split("\n");
        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (count >= 10) break;
            summary.append(line).append("\n");
            count++;
        }
        return summary.toString();
    }

    private String buildArtifactContent(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Build status: ").append(result.status()).append("\n");
        sb.append("Exit code: ").append(result.exitCode() != null ? result.exitCode() : "N/A").append("\n");
        sb.append("Duration: ").append(result.durationMs()).append(" ms\n");
        if (result.errorMessage() != null) {
            sb.append("Error: ").append(result.errorMessage()).append("\n");
        }
        sb.append("\n[stdout]\n").append(result.stdout());
        if (!result.stderr().isBlank()) {
            sb.append("\n\n[stderr]\n").append(result.stderr());
        }
        return sb.toString();
    }

    private WorkflowStepResult stopFailure(String message) {
        String cause = (message == null || message.isBlank()) ? "unexpected failure" : message;
        return new WorkflowStepResult(
                WorkflowStepDecision.STOP_FAILURE,
                ERROR_PREFIX + cause,
                Map.of(
                        "finalDecision", WorkflowStepDecision.STOP_FAILURE.name(),
                        "nextAction", STOP_FAILURE_NEXT_ACTION
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

    private int optionalInt(WorkflowExecutionContext context, String key, int defaultValue) {
        Object value = context.variables().get(key);
        if (value == null) {
            return defaultValue;
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
