package fr.ses10doigts.toolkitbridge.memory.facade.model;

public record ToolExecutionRecord(
        String toolName,
        boolean success,
        String details
) {
    public ToolExecutionRecord {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        details = details == null ? "" : details.trim();
    }
}
