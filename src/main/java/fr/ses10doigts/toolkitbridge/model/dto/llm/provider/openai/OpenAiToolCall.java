package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

public record OpenAiToolCall(
        String id,
        String type,
        OpenAiToolFunction function
) {
}