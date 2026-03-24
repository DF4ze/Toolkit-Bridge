package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

public record AgentDefinition(
        String id,
        String name,
        String telegramBotId,
        AgentOrchestratorType  orchestratorType,
        String llmProvider,
        String model,
        String systemPrompt
) {
}