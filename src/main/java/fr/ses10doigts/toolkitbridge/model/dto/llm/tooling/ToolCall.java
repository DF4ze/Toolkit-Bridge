package fr.ses10doigts.toolkitbridge.model.dto.llm.tooling;

public record ToolCall(
        String id,
        ToolFunction function
) {
}
