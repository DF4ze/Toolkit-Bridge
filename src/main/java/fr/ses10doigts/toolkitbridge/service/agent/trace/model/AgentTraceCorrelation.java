package fr.ses10doigts.toolkitbridge.service.agent.trace.model;

public record AgentTraceCorrelation(
        String runId,
        String taskId,
        String agentId,
        String messageId
) {
}
