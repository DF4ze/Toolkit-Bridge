package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.AnalysisReviewWorkflowRunner;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact.WorkflowArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex.CodexWorkflowClient;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.orchestrator.WorkflowOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.BuildErrorCorrectionStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.CorrectionStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.GlobalAnalysisStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.GlobalReviewStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.MavenValidationStep;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation.WorkflowValidationService;

public final class AnalysisReviewWorkflowRunnerFactory {

    private AnalysisReviewWorkflowRunnerFactory() {
    }

    public static AnalysisReviewWorkflowRunner createDefault() {
        WorkflowArtifactService artifactService = new WorkflowArtifactService();
        CodexWorkflowClient codexWorkflowClient = new CodexWorkflowClient();
        return new AnalysisReviewWorkflowRunner(
                new WorkflowOrchestrator(),
                artifactService,
                new GlobalAnalysisStep(codexWorkflowClient, artifactService),
                new GlobalReviewStep(codexWorkflowClient, artifactService),
                new CorrectionStep(codexWorkflowClient, artifactService),
                new MavenValidationStep(new WorkflowValidationService(), artifactService),
                new BuildErrorCorrectionStep(codexWorkflowClient, artifactService)
        );
    }
}
