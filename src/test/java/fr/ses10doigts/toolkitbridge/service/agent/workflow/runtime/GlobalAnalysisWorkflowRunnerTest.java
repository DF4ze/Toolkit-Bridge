package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.GlobalAnalysisStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GlobalAnalysisWorkflowRunnerTest {

    @Test
    void runsWorkflowLocallyAndGeneratesAnalysisArtifacts() throws Exception {
        assumeTrue(isCodexAvailable(), "Skipping local workflow runner test because codex CLI is not available");

        Path analysisSourcePath = Path.of("data", "rapport", "00.promptWorkflow", "0.workflow.md");
        assumeTrue(Files.exists(analysisSourcePath), "Skipping local workflow runner test because source document is missing");

        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CodexWorkflowClient codexClient = new CodexWorkflowClient();
        GlobalAnalysisStep step = new GlobalAnalysisStep(codexClient, artifactService);
        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator();

        WorkflowRun workflowRun = new WorkflowRun(
                "run-phase2-step3",
                "codex-implementation",
                "phase-2/step-3",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        WorkflowExecutionContext context = new WorkflowExecutionContext(
                workflowRun,
                null,
                Map.of(
                        "reportRootDirectory", Path.of("data", "rapport"),
                        "reportVersion", "v1.1",
                        "reportPhase", "CodexTime/Phase2",
                        "stepNumber", 3,
                        "analysisSourcePath", analysisSourcePath,
                        "codexWorkingDirectory", Path.of("."),
                        "codexTimeoutSeconds", 120
                )
        );

        WorkflowStepResult result = orchestrator.executeSingleStep(context, step);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.data()).containsKeys("promptArtifactPath", "resultArtifactPath");

        Path promptArtifactPath = Path.of(result.data().get("promptArtifactPath").toString());
        Path resultArtifactPath = Path.of(result.data().get("resultArtifactPath").toString());

        assertThat(promptArtifactPath.getFileName().toString()).isEqualTo("3.analysis.md");
        assertThat(resultArtifactPath.getFileName().toString()).isEqualTo("result.3.analysis.md");

        assertThat(Files.exists(promptArtifactPath)).isTrue();
        assertThat(Files.exists(resultArtifactPath)).isTrue();

        String promptContent = Files.readString(promptArtifactPath, StandardCharsets.UTF_8);
        String resultContent = Files.readString(resultArtifactPath, StandardCharsets.UTF_8);

        assertThat(promptContent).isNotBlank();
        assertThat(promptContent).contains("Global analysis request");
        assertThat(resultContent).isNotBlank();
    }

    private boolean isCodexAvailable() {
        try {
            Process process = new ProcessBuilder("codex", "--version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
