package fr.ses10doigts.toolkitbridge.model.dto.llm.tool;

import java.util.Map;

public record ToolSpec(
        String name,
        String description,
        Map<String, Object> parameters
) {
}
