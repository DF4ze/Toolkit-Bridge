package fr.ses10doigts.toolkitbridge.config.llm;

public record OpenAiLikeProperties(
        String name,
        String baseUrl,
        String apiKey,
        String defaultModel
) {
}