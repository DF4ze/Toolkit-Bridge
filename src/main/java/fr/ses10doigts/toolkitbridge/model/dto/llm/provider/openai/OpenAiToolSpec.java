package fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai;

import java.util.Map;

public record OpenAiToolSpec(
        String name,
        String description,
        Map<String, Object> parameters
) {
}