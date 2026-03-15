package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

public record OpenAiChoice(
        int index,
        OpenAiMessage message,
        String finish_reason
) {
}