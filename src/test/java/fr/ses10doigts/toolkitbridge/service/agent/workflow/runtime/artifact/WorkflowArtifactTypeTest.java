package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.artifact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowArtifactTypeTest {

    @Test
    void buildsExpectedFileNames() {
        assertThat(WorkflowArtifactType.ANALYSIS_PROMPT.fileNameForStep(4)).isEqualTo("4.analysis.md");
        assertThat(WorkflowArtifactType.ANALYSIS_RESULT.fileNameForStep(4)).isEqualTo("result.4.analysis.md");
        assertThat(WorkflowArtifactType.IMPLEMENTS_PROMPT.fileNameForStep(4)).isEqualTo("4.implements.md");
        assertThat(WorkflowArtifactType.REVIEW_RESULT.fileNameForStep(4)).isEqualTo("result.4.review.md");
        assertThat(WorkflowArtifactType.WORKFLOW_SUMMARY.fileNameForStep(4)).isEqualTo("workflow-summary.md");
    }

    @Test
    void rejectsInvalidStepNumber() {
        assertThatThrownBy(() -> WorkflowArtifactType.ANALYSIS_PROMPT.fileNameForStep(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stepNumber");
    }

    @Test
    void workflowSummaryDoesNotRequireStepNumber() {
        assertThat(WorkflowArtifactType.WORKFLOW_SUMMARY.fileNameForStep(0)).isEqualTo("workflow-summary.md");
    }
}
