package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

public record TaskPrompt(
        String systemPrompt,
        String userPrompt
) {
}
