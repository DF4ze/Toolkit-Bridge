package fr.ses10doigts.toolkitbridge.model.dto.llm;

import java.util.Map;

public record OllamaToolFunction(
        String name,
        Map<String, Object> arguments
) {
}
