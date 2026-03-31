package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

public record AgentDefinition(
        String id,
        String name,
        String telegramBotId,
        AgentOrchestratorType  orchestratorType,
        String llmProvider,
        String model,
        String systemPrompt,
        boolean toolsEnabled
) {
    public static AgentDefinition fromProperties( AgentDefinitionProperties props ){
        return new AgentDefinition(
                props.getId(),
                props.getName(),
                props.getTelegramBotId(),
                AgentOrchestratorType.valueOf( props.getOrchestratorType().toUpperCase() ),
                props.getLlmProvider(),
                props.getModel(),
                props.getSystemPrompt(),
                props.getToolsEnabled() == null || props.getToolsEnabled()
        );
    }
}
