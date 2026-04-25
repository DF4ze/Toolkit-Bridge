package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorkflowCliConsumerSimulator {

    private static final String DECISION = "decision";
    private static final String FINAL_DECISION = "finalDecision";
    private static final String MESSAGE = "message";
    private static final String NEXT_ACTION = "nextAction";
    private static final String WAIT_REASON = "waitReason";
    private static final String WORKFLOW_SUMMARY_PATH = "workflowSummaryPath";
    private static final Set<String> KNOWN_KEYS = Set.of(
            DECISION,
            FINAL_DECISION,
            MESSAGE,
            NEXT_ACTION,
            "correctionTriggered",
            WAIT_REASON,
            WORKFLOW_SUMMARY_PATH
    );

    public Map<String, String> parse(String cliOutput) {
        if (cliOutput == null || cliOutput.isBlank()) {
            return Map.of();
        }

        Map<String, String> parsed = new HashMap<>();
        for (String rawLine : cliOutput.split("\\R")) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            int separatorIndex = rawLine.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = rawLine.substring(0, separatorIndex).trim();
            if (!KNOWN_KEYS.contains(key)) {
                continue;
            }
            String value = rawLine.substring(separatorIndex + 1).trim();
            parsed.put(key, value);
        }
        return Map.copyOf(parsed);
    }

    public SimulatedConsumerResult consume(int exitCode, String cliOutput) {
        Map<String, String> fields = parse(cliOutput);
        if (exitCode != 0) {
            return new SimulatedConsumerResult(
                    SimulatedConsumerStatus.ERROR,
                    fields,
                    value(fields, MESSAGE),
                    value(fields, NEXT_ACTION),
                    value(fields, WAIT_REASON),
                    value(fields, WORKFLOW_SUMMARY_PATH)
            );
        }

        String decision = firstNonBlank(fields.get(FINAL_DECISION), fields.get(DECISION));
        SimulatedConsumerStatus status = switch (decision) {
            case "WAIT_HUMAN" -> SimulatedConsumerStatus.WAITING_FOR_HUMAN;
            case "STOP_FAILURE" -> SimulatedConsumerStatus.FAILED;
            case "CONTINUE" -> SimulatedConsumerStatus.SUCCESS;
            case null, default -> SimulatedConsumerStatus.UNKNOWN;
        };

        return new SimulatedConsumerResult(
                status,
                fields,
                value(fields, MESSAGE),
                value(fields, NEXT_ACTION),
                value(fields, WAIT_REASON),
                value(fields, WORKFLOW_SUMMARY_PATH)
        );
    }

    private String value(Map<String, String> fields, String key) {
        return fields.getOrDefault(key, "");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    public enum SimulatedConsumerStatus {
        WAITING_FOR_HUMAN,
        FAILED,
        SUCCESS,
        UNKNOWN,
        ERROR
    }

    public record SimulatedConsumerResult(
            SimulatedConsumerStatus status,
            Map<String, String> fields,
            String message,
            String nextAction,
            String waitReason,
            String workflowSummaryPath
    ) {
    }
}
