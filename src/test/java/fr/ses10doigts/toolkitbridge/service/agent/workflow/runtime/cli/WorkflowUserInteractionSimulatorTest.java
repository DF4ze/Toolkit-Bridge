package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.WorkflowCliConsumerSimulator.SimulatedConsumerResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowUserInteractionSimulatorTest {

    private final WorkflowCliConsumerSimulator consumer = new WorkflowCliConsumerSimulator();
    private final WorkflowUserInteractionSimulator interaction = new WorkflowUserInteractionSimulator();

    @Test
    void renderRunWaitHumanMessage() {
        SimulatedConsumerResult result = consumer.consume(0, """
                decision=WAIT_HUMAN
                finalDecision=WAIT_HUMAN
                message=Global review requires human decision
                nextAction=Edit review result and resume with runCorrectionAfterReview(...)
                correctionTriggered=false
                waitReason=Missing decision in review
                workflowSummaryPath=D:\\reports\\workflow-summary.md
                """);

        String message = interaction.renderMessage(result);

        assertThat(message)
                .contains("Status: WAITING_FOR_HUMAN")
                .contains("Message: Global review requires human decision")
                .contains("Reason: Missing decision in review")
                .contains("Next action: Edit review result and resume with runCorrectionAfterReview(...)")
                .contains("Summary: D:\\reports\\workflow-summary.md");
    }

    @Test
    void renderWaitHumanDoesNotTriggerResumeAutomatically() {
        SimulatedConsumerResult result = consumer.consume(0, """
                decision=WAIT_HUMAN
                finalDecision=WAIT_HUMAN
                message=Review requires intervention
                nextAction=Edit review result and resume manually
                """);

        String message = interaction.renderMessage(result);

        assertThat(result.status().name()).isEqualTo("WAITING_FOR_HUMAN");
        assertThat(message)
                .contains("Status: WAITING_FOR_HUMAN")
                .doesNotContain("Status: SUCCESS")
                .doesNotContain("resume executed");
    }

    @Test
    void renderResumeSuccessMessage() {
        SimulatedConsumerResult result = consumer.consume(0, """
                decision=CONTINUE
                finalDecision=CONTINUE
                message=Correction completed
                nextAction=Continue workflow execution
                correctionTriggered=true
                workflowSummaryPath=D:\\reports\\workflow-summary.md
                """);

        String message = interaction.renderMessage(result);

        assertThat(message)
                .contains("Status: SUCCESS")
                .contains("Message: Correction completed")
                .contains("Next action: Continue workflow execution")
                .contains("Summary: D:\\reports\\workflow-summary.md")
                .doesNotContain("Reason:");
    }

    @Test
    void renderStopFailureMessage() {
        SimulatedConsumerResult result = consumer.consume(0, """
                decision=STOP_FAILURE
                finalDecision=STOP_FAILURE
                message=Correction failed
                nextAction=Inspect failure and related artifacts, then rerun
                """);

        String message = interaction.renderMessage(result);

        assertThat(message)
                .contains("Status: FAILED")
                .contains("Message: Correction failed")
                .contains("Next action: Inspect failure and related artifacts, then rerun");
    }

    @Test
    void renderTechnicalErrorMessage() {
        SimulatedConsumerResult result = consumer.consume(2, """
                message=Missing required argument: reportVersion
                """);

        String message = interaction.renderMessage(result);

        assertThat(message)
                .contains("Status: ERROR")
                .contains("Message: Missing required argument: reportVersion");
    }

    @Test
    void renderUnknownDecisionMessage() {
        SimulatedConsumerResult result = consumer.consume(0, """
                decision=FINISH
                finalDecision=FINISH
                message=Unexpected decision
                """);

        String message = interaction.renderMessage(result);

        assertThat(message)
                .contains("Status: UNKNOWN")
                .contains("Message: Unexpected decision");
    }

    @Test
    void renderNullResultAsUnknownMessage() {
        String message = interaction.renderMessage(null);

        assertThat(message)
                .contains("Status: UNKNOWN")
                .contains("Message: No workflow result available");
    }
}
