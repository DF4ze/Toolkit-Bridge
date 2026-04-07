package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

public record AgentDefinition(
        String id,
        String name,
        String telegramBotId,
        AgentRole role,
        AgentOrchestratorType orchestratorType,
        String llmProvider,
        String model,
        String systemPrompt,
        String policyName,
        boolean toolsEnabled,
        AgentPolicyDefinition policy
) {
    public AgentDefinition(
            String id,
            String name,
            String telegramBotId,
            AgentRole role,
            AgentOrchestratorType orchestratorType,
            String llmProvider,
            String model,
            String systemPrompt,
            String policyName,
            boolean toolsEnabled
    ) {
        this(
                id,
                name,
                telegramBotId,
                role,
                orchestratorType,
                llmProvider,
                model,
                systemPrompt,
                policyName,
                toolsEnabled,
                new AgentPolicyProperties().toDefinition()
        );
    }

    public static AgentDefinition fromProperties(AgentDefinitionProperties props) {
        return new AgentDefinition(
                props.getId(),
                props.getName(),
                props.getTelegramBotId(),
                AgentRole.valueOf(props.getRole().trim().toUpperCase()),
                AgentOrchestratorType.valueOf(props.getOrchestratorType().trim().toUpperCase()),
                props.getLlmProvider(),
                props.getModel(),
                props.getSystemPrompt(),
                props.getPolicyName(),
                props.getToolsEnabled() == null || props.getToolsEnabled(),
                props.getPolicy() == null ? new AgentPolicyProperties().toDefinition() : props.getPolicy().toDefinition()
        );
    }
}
