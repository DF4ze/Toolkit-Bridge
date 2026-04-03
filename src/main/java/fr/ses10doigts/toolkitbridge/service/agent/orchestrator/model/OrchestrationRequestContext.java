package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.model;

public record OrchestrationRequestContext(
        String agentId,
        String conversationId,
        String traceId,
        String projectId,
        String userMessage,
        boolean toolsEnabled
) {
}
