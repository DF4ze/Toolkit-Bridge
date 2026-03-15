package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

public record OpenAiToolDefinition(
        String type,
        OpenAiToolSpec function
) {
}