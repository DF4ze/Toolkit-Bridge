package fr.ses10doigts.toolkitbridge.model.dto.llm;

import java.util.Map;

public record OllamaToolSpec(
        String name,
        String description,
        Map<String, Object> parameters
) {
}
