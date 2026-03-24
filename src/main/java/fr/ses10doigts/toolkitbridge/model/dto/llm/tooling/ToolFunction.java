package fr.ses10doigts.toolkitbridge.model.dto.llm.tooling;

import java.util.Map;

public record ToolFunction(
        String name,
        Map<String, Object> arguments
) {
}
