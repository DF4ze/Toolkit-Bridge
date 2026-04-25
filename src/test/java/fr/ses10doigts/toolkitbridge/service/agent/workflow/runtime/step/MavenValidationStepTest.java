package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.ValidationStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.WorkflowValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MavenValidationStepTest {

    @TempDir
    Path tempDir;

    // --- helpers ---

    private WorkflowValidationService mockServiceReturning(ValidationStatus status,
                                                           Integer exitCode,
                                                           String errorMessage) {
        WorkflowValidationService service = mock(WorkflowValidationService.class);
        when(service.validate(any())).thenReturn(new ValidationResult(
                "cmd.exe /c mvnw.cmd clean compile -DskipTests",
                status,
                exitCode,
                "build output",
                "",
                1234L,
                errorMessage
        ));
        return service;
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-test",
                "codex-implementation",
                "phase-6/step-2",
                WorkflowRunStatus.RUNNING,
                null, null, null, null, null, null, null
        );
        return new WorkflowExecutionContext(workflowRun, null, variables);
    }

    private Map<String, Object> baseVariables() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("reportRootDirectory", tempDir.resolve("data/rapport"));
        vars.put("reportVersion", "v1.1");
        vars.put("reportPhase", "CodexTime/Phase6");
        vars.put("stepNumber", 2);
        vars.put("validationWorkingDirectory", tempDir);
        return vars;
    }

    // --- SUCCESS ---

    @Test
    void returnsContinueOnSuccess() {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.SUCCESS, 0, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Maven build validation passed");
        assertThat(result.data()).containsEntry("buildStatus", "SUCCESS");
        assertThat(result.data()).containsEntry("buildExitCode", 0);
        assertThat(result.data()).containsKey("buildResultPath");
        assertThat(result.data()).containsKey("buildDurationMs");
    }

    @Test
    void buildArtifactIsWrittenOnSuccess() throws Exception {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.SUCCESS, 0, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        Path artifactPath = Path.of(result.data().get("buildResultPath").toString());
        assertThat(Files.exists(artifactPath)).isTrue();
        assertThat(Files.readString(artifactPath)).contains("Build status: SUCCESS");
    }

    // --- FAILURE ---

    @Test
    void returnsRetryCorrectionOnBuildFailureWhenRetryIsAvailable() {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.FAILURE, 1, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.RETRY_CORRECTION);
        assertThat(result.message()).contains("Retry count: 1/2");
        assertThat(result.data()).containsEntry("buildStatus", "FAILURE");
        assertThat(result.data()).containsEntry("buildExitCode", 1);
        assertThat(result.data()).containsEntry("buildErrorRetryCount", 1);
        assertThat(result.data()).containsEntry("buildErrorRetryMax", 2);
        assertThat(result.data()).containsKey("buildErrorSummary");
        assertThat(result.data()).containsKey("buildArtifactPath");
    }

    @Test
    void returnsStopFailureOnBuildFailureWhenRetryMaxIsReached() {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.FAILURE, 1, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());
        Map<String, Object> vars = baseVariables();
        vars.put("buildErrorRetryCount", 2);
        vars.put("buildErrorRetryMax", 2);

        WorkflowStepResult result = step.execute(buildContext(vars));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).startsWith("MavenValidationStep:");
        assertThat(result.data()).containsEntry("buildStatus", "FAILURE");
        assertThat(result.data()).containsEntry("buildExitCode", 1);
    }

    // --- TIMEOUT ---

    @Test
    void returnsStopFailureOnTimeout() {
        WorkflowValidationService service = mockServiceReturning(
                ValidationStatus.TIMEOUT, null, "Process timed out after 300 seconds");
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.data()).containsEntry("buildStatus", "TIMEOUT");
        assertThat(result.data()).doesNotContainKey("buildExitCode");
    }

    // --- SYSTEM_ERROR ---

    @Test
    void returnsStopFailureOnSystemError() {
        WorkflowValidationService service = mockServiceReturning(
                ValidationStatus.SYSTEM_ERROR, null, "Failed to start process: mvnw.cmd not found");
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.data()).containsEntry("buildStatus", "SYSTEM_ERROR");
        assertThat(result.data()).doesNotContainKey("buildExitCode");
    }

    // --- null context ---

    @Test
    void returnsStopFailureOnNullContext() {
        WorkflowValidationService service = mock(WorkflowValidationService.class);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(null);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("context must not be null");
    }

    // --- missing variables ---

    @Test
    void returnsStopFailureWhenValidationWorkingDirectoryMissing() {
        WorkflowValidationService service = mock(WorkflowValidationService.class);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        Map<String, Object> vars = baseVariables();
        vars.remove("validationWorkingDirectory");

        WorkflowStepResult result = step.execute(buildContext(vars));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("validationWorkingDirectory");
    }

    @Test
    void returnsStopFailureWhenReportRootDirectoryMissing() {
        WorkflowValidationService service = mock(WorkflowValidationService.class);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        Map<String, Object> vars = baseVariables();
        vars.remove("reportRootDirectory");

        WorkflowStepResult result = step.execute(buildContext(vars));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("reportRootDirectory");
    }

    // --- data contract ---

    @Test
    void resultAlwaysContainsMandatoryKeys() {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.SUCCESS, 0, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.data()).containsKeys("buildStatus", "buildDurationMs", "buildResultPath",
                "finalDecision", "nextAction");
    }

    // --- optional timeout ---

    @Test
    void acceptsCustomTimeout() {
        WorkflowValidationService service = mockServiceReturning(ValidationStatus.SUCCESS, 0, null);
        MavenValidationStep step = new MavenValidationStep(service, new WorkflowArtifactService());

        Map<String, Object> vars = baseVariables();
        vars.put("validationTimeoutSeconds", 60);

        WorkflowStepResult result = step.execute(buildContext(vars));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
    }
}
