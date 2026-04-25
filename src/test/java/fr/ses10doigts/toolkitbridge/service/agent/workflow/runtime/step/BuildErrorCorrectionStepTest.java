package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionRequest;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildErrorCorrectionStepTest {

    @TempDir
    Path tempDir;

    @Test
    void executesCodexWithBuildErrorContextAndReturnsContinue() throws Exception {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex",
                0,
                "fixed code",
                "",
                true,
                false,
                100L
        ));
        BuildErrorCorrectionStep step = new BuildErrorCorrectionStep(codexClient, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("Build error analysis and correction attempted");
        assertThat(result.data())
                .containsEntry("finalDecision", "CONTINUE")
                .containsEntry("correctionTriggered", true)
                .containsEntry("buildArtifactPath", "result.3.build.md")
                .containsEntry("buildErrorRetryCount", 1)
                .containsEntry("buildErrorRetryMax", 2)
                .containsKeys("resultArtifactPath", "correctionAttemptPath", "nextAction");
        assertThat(result.data()).doesNotContainKey("promptArtifactPath");
        assertThat(Files.readString(Path.of(result.data().get("resultArtifactPath").toString())))
                .contains("Attempt: 1 / 2")
                .contains("fixed code");

        ArgumentCaptor<CodexExecutionRequest> requestCaptor = ArgumentCaptor.forClass(CodexExecutionRequest.class);
        verify(codexClient).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().prompt())
                .contains("Compilation failure")
                .contains("result.3.build.md");
    }

    @Test
    void returnsStopFailureWhenBuildErrorSummaryIsMissing() {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        BuildErrorCorrectionStep step = new BuildErrorCorrectionStep(codexClient, new WorkflowArtifactService());
        Map<String, Object> variables = baseVariables();
        variables.remove("buildErrorSummary");

        WorkflowStepResult result = step.execute(buildContext(variables));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("buildErrorSummary");
        assertThat(result.data())
                .containsEntry("finalDecision", "STOP_FAILURE")
                .containsEntry("correctionTriggered", false)
                .containsKey("nextAction");
    }

    @Test
    void returnsStopFailureWhenCodexFails() {
        CodexWorkflowClient codexClient = mock(CodexWorkflowClient.class);
        when(codexClient.execute(any())).thenReturn(new CodexExecutionResult(
                "codex",
                1,
                "",
                "error",
                false,
                false,
                100L
        ));
        BuildErrorCorrectionStep step = new BuildErrorCorrectionStep(codexClient, new WorkflowArtifactService());

        WorkflowStepResult result = step.execute(buildContext(baseVariables()));

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.STOP_FAILURE);
        assertThat(result.message()).contains("Codex execution was not successful");
    }

    private Map<String, Object> baseVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("reportRootDirectory", tempDir.resolve("data/rapport"));
        variables.put("reportVersion", "v1.1");
        variables.put("reportPhase", "CodexTime/Phase6/etape3");
        variables.put("stepNumber", 3);
        variables.put("buildErrorSummary", "Compilation failure");
        variables.put("buildArtifactPath", "result.3.build.md");
        variables.put("buildErrorRetryCount", 1);
        variables.put("buildErrorRetryMax", 2);
        return variables;
    }

    private WorkflowExecutionContext buildContext(Map<String, Object> variables) {
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
        return new WorkflowExecutionContext(workflowRun, null, variables);
    }
}
