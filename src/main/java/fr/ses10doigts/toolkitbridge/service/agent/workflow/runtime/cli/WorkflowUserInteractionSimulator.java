package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.WorkflowCliConsumerSimulator.SimulatedConsumerResult;

public class WorkflowUserInteractionSimulator {

    public String renderMessage(SimulatedConsumerResult result) {
        if (result == null) {
            return "Status: UNKNOWN\nMessage: No workflow result available";
        }

        StringBuilder message = new StringBuilder();
        message.append("Status: ").append(result.status()).append("\n");
        appendLine(message, "Message", result.message());
        appendLine(message, "Reason", result.waitReason());
        appendLine(message, "Next action", result.nextAction());
        appendLine(message, "Summary", result.workflowSummaryPath());
        return message.toString();
    }

    private void appendLine(StringBuilder message, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        message.append(label).append(": ").append(value.trim()).append("\n");
    }
}
