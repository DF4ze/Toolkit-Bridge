package fr.ses10doigts.toolkitbridge.model.dto.llm.tooling;

import java.util.Map;

public record ToolSpec(
        String name,
        String description,
        Map<String, Object> parameters
) {
}
