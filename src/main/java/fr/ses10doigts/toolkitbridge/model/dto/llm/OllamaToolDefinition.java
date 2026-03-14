package fr.ses10doigts.toolkitbridge.model.dto.llm;

public record OllamaToolDefinition(
        String type,
        OllamaToolSpec function
) {
    public static OllamaToolDefinition function(OllamaToolSpec spec) {
        return new OllamaToolDefinition("function", spec);
    }
}
