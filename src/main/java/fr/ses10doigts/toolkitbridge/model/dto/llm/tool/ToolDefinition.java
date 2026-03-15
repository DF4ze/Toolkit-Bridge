package fr.ses10doigts.toolkitbridge.model.dto.llm.tool;

public record ToolDefinition(
        String type,
        ToolSpec function
) {
    public static ToolDefinition function(ToolSpec spec) {
        return new ToolDefinition("function", spec);
    }
}
