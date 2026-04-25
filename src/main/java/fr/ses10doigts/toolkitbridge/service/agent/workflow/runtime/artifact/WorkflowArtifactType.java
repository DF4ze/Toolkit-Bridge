package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact;

public enum WorkflowArtifactType {
    ANALYSIS_PROMPT("analysis", false),
    ANALYSIS_RESULT("analysis", true),
    IMPLEMENTS_PROMPT("implements", false),
    IMPLEMENTS_RESULT("implements", true),
    REVIEW_PROMPT("review", false),
    REVIEW_RESULT("review", true),
    CORRECTION_PROMPT("correction", false),
    CORRECTION_RESULT("correction", true),
    BUILD_RESULT("build", true),
    BUILD_ERROR_CORRECTION("build-error-correction", true),
    WORKFLOW_SUMMARY("workflow-summary", true);

    private final String suffix;
    private final boolean resultArtifact;

    WorkflowArtifactType(String suffix, boolean resultArtifact) {
        this.suffix = suffix;
        this.resultArtifact = resultArtifact;
    }

    public String fileNameForStep(int stepNumber) {
        if (this == WORKFLOW_SUMMARY) {
            return "workflow-summary.md";
        }
        if (stepNumber <= 0) {
            throw new IllegalArgumentException("stepNumber must be greater than 0");
        }
        if (resultArtifact) {
            return "result." + stepNumber + "." + suffix + ".md";
        }
        return stepNumber + "." + suffix + ".md";
    }
}
