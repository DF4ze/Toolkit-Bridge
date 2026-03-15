package fr.ses10doigts.toolkitbridge.model.dto.llm.tool;

public record ToolCall(
        String id,
        ToolFunction function
) {
}
