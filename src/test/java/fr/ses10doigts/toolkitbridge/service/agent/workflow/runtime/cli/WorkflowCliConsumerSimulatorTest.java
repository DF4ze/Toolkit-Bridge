package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.WorkflowCliConsumerSimulator.SimulatedConsumerResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.WorkflowCliConsumerSimulator.SimulatedConsumerStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowCliConsumerSimulatorTest {

    private final WorkflowCliConsumerSimulator simulator = new WorkflowCliConsumerSimulator();

    @Test
    void parseReadsKnownKeyValueLines() {
        Map<String, String> parsed = simulator.parse("""
                decision=WAIT_HUMAN
                message=Review needs a decision
                nextAction=Edit review result
                workflowSummaryPath=D:\\reports\\workflow-summary.md
                """);

        assertThat(parsed)
                .containsEntry("decision", "WAIT_HUMAN")
                .containsEntry("message", "Review needs a decision")
                .containsEntry("nextAction", "Edit review result")
                .containsEntry("workflowSummaryPath", "D:\\reports\\workflow-summary.md");
    }

    @Test
    void parseIgnoresInvalidAndUnknownLines() {
        Map<String, String> parsed = simulator.parse("""
                invalid line
                unknown=value
                decision=CONTINUE

                =missingKey
                """);

        assertThat(parsed)
                .containsOnly(Map.entry("decision", "CONTINUE"));
    }

    @Test
    void consumeUsesFallbackDecisionWhenFinalDecisionIsMissing() {
        SimulatedConsumerResult result = simulator.consume(0, """
                decision=CONTINUE
                message=Done
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("Done");
        assertThat(result.nextAction()).isEmpty();
    }

    @Test
    void consumeWaitHumanResult() {
        SimulatedConsumerResult result = simulator.consume(0, """
                decision=WAIT_HUMAN
                finalDecision=WAIT_HUMAN
                message=Global review requires human decision
                nextAction=Edit review result and resume with runCorrectionAfterReview(...)
                correctionTriggered=false
                waitReason=Missing decision in review
                workflowSummaryPath=D:\\reports\\workflow-summary.md
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.WAITING_FOR_HUMAN);
        assertThat(result.message()).isEqualTo("Global review requires human decision");
        assertThat(result.nextAction()).isEqualTo("Edit review result and resume with runCorrectionAfterReview(...)");
        assertThat(result.waitReason()).isEqualTo("Missing decision in review");
        assertThat(result.workflowSummaryPath()).isEqualTo("D:\\reports\\workflow-summary.md");
    }

    @Test
    void consumeStopFailureResult() {
        SimulatedConsumerResult result = simulator.consume(0, """
                decision=STOP_FAILURE
                finalDecision=STOP_FAILURE
                message=Correction failed
                nextAction=Inspect failure and related artifacts, then rerun
                correctionTriggered=false
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.FAILED);
        assertThat(result.message()).isEqualTo("Correction failed");
        assertThat(result.nextAction()).isEqualTo("Inspect failure and related artifacts, then rerun");
    }

    @Test
    void consumeContinueResult() {
        SimulatedConsumerResult result = simulator.consume(0, """
                decision=CONTINUE
                finalDecision=CONTINUE
                message=Correction completed
                nextAction=Continue workflow execution
                correctionTriggered=true
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.SUCCESS);
        assertThat(result.fields()).containsEntry("correctionTriggered", "true");
    }

    @Test
    void consumeUnknownDecisionResult() {
        SimulatedConsumerResult result = simulator.consume(0, """
                decision=FINISH
                finalDecision=FINISH
                message=Unexpected decision
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.UNKNOWN);
        assertThat(result.message()).isEqualTo("Unexpected decision");
    }

    @Test
    void consumeInvalidCliOutputReturnsUnknown() {
        SimulatedConsumerResult result = simulator.consume(0, """
                invalid line
                another invalid line
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.UNKNOWN);
        assertThat(result.fields()).isEmpty();
    }

    @Test
    void consumeNonZeroExitCodeReturnsError() {
        SimulatedConsumerResult result = simulator.consume(2, """
                message=Missing required argument: reportVersion
                """);

        assertThat(result.status()).isEqualTo(SimulatedConsumerStatus.ERROR);
        assertThat(result.message()).isEqualTo("Missing required argument: reportVersion");
    }
}
