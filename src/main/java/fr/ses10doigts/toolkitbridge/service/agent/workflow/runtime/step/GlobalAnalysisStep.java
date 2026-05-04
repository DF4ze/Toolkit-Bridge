package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionException;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionRequest;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class GlobalAnalysisStep implements WorkflowStep {

    private static final String ERROR_PREFIX = "GlobalAnalysisStep: ";
    private static final String COMPLETED_MESSAGE = "Global analysis completed";
    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_ANALYSIS_SOURCE_PATH = "analysisSourcePath";
    private static final String VAR_CODEX_WORKING_DIRECTORY = "codexWorkingDirectory";
    private static final String VAR_CODEX_TIMEOUT_SECONDS = "codexTimeoutSeconds";

    private final CodexWorkflowClient codexWorkflowClient;
    private final WorkflowArtifactService workflowArtifactService;

    public GlobalAnalysisStep(CodexWorkflowClient codexWorkflowClient,
                              WorkflowArtifactService workflowArtifactService) {
        this.codexWorkflowClient = Objects.requireNonNull(codexWorkflowClient, "codexWorkflowClient must not be null");
        this.workflowArtifactService = Objects.requireNonNull(workflowArtifactService, "workflowArtifactService must not be null");
    }

    @Override
    public WorkflowStepResult execute(WorkflowExecutionContext context) {
        if (context == null) {
            return stopFailure("context must not be null");
        }

        String runId = context.workflowRun().runId();
        try {
            Path reportRootDirectory = requiredPath(context, VAR_REPORT_ROOT_DIRECTORY);
            String reportVersion = requiredString(context, VAR_REPORT_VERSION);
            String reportPhase = requiredString(context, VAR_REPORT_PHASE);
            int stepNumber = requiredInt(context, VAR_STEP_NUMBER);
            Path analysisSourcePath = requiredPath(context, VAR_ANALYSIS_SOURCE_PATH);

            String sourceContent = Files.readString(analysisSourcePath, StandardCharsets.UTF_8);
            String prompt = buildPrompt(context, analysisSourcePath, sourceContent);

            Path promptArtifactPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.ANALYSIS_PROMPT
            );
            workflowArtifactService.writeArtifact(promptArtifactPath, prompt);

            CodexExecutionRequest request = new CodexExecutionRequest(
                    prompt,
                    optionalPath(context, VAR_CODEX_WORKING_DIRECTORY),
                    optionalInt(context, VAR_CODEX_TIMEOUT_SECONDS)
            );

            log.info("Codex analysis call started: runId={}", runId);
            CodexExecutionResult codexResult = codexWorkflowClient.execute(request);
            log.debug("Codex analysis call completed: runId={}, exitCode={}, durationMs={}, stdoutLen={}, stderrLen={}",
                    runId, codexResult.exitCode(), codexResult.durationMs(),
                    codexResult.stdout().length(), codexResult.stderr().length());

            String resultContent = buildResultContent(codexResult);

            Path resultArtifactPath = workflowArtifactService.buildArtifactPath(
                    reportRootDirectory,
                    reportVersion,
                    reportPhase,
                    stepNumber,
                    WorkflowArtifactType.ANALYSIS_RESULT
            );
            workflowArtifactService.writeArtifact(resultArtifactPath, resultContent);

            if (!codexResult.success()) {
                if (codexResult.timedOut()) {
                    log.warn("Codex analysis timed out: runId={}, durationMs={}", runId, codexResult.durationMs());
                } else {
                    log.warn("Codex analysis failed: runId={}, exitCode={}", runId, codexResult.exitCode());
                }
                String failureMessage = codexResult.timedOut()
                        ? "Codex execution timed out"
                        : "Codex execution was not successful";
                return new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        ERROR_PREFIX + failureMessage,
                        Map.of(
                                "promptArtifactPath", promptArtifactPath.toString(),
                                "resultArtifactPath", resultArtifactPath.toString()
                        )
                );
            }

            log.info("Codex analysis call succeeded: runId={}", runId);
            return new WorkflowStepResult(
                    WorkflowStepDecision.CONTINUE,
                    COMPLETED_MESSAGE,
                    Map.of(
                            "promptArtifactPath", promptArtifactPath.toString(),
                            "resultArtifactPath", resultArtifactPath.toString()
                    )
            );
        } catch (IllegalArgumentException | IOException | CodexExecutionException | IllegalStateException e) {
            log.error("GlobalAnalysisStep failed: runId={}", runId, e);
            return stopFailure(e.getMessage());
        }
    }

    private String buildPrompt(WorkflowExecutionContext context, Path sourcePath, String sourceContent) {
        return "Global analysis request\n"
                + "runId: " + context.workflowRun().runId() + "\n"
                + "workflowType: " + context.workflowRun().workflowType() + "\n"
                + "targetStepRef: " + context.workflowRun().targetStepRef() + "\n"
                + "sourcePath: " + sourcePath + "\n\n"
                + sourceContent;
    }

    private String buildResultContent(CodexExecutionResult result) {
        if (result.stderr().isBlank()) {
            return result.stdout();
        }
        return result.stdout() + "\n\n[stderr]\n" + result.stderr();
    }

    private WorkflowStepResult stopFailure(String message) {
        String cause = (message == null || message.isBlank()) ? "unexpected failure" : message;
        return new WorkflowStepResult(WorkflowStepDecision.STOP_FAILURE, ERROR_PREFIX + cause, Map.of());
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
