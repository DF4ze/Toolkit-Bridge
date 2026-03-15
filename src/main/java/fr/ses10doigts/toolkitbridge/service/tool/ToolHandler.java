package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;

import java.util.Map;

public interface ToolHandler {
    String name();
    String description();
    Map<String, Object> parametersSchema();
    ToolExecutionResult execute(Map<String, Object> arguments) throws Exception;
}