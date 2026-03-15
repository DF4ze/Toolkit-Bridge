package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

public record OpenAiToolFunction(
        String name,
        String arguments
) {
}