package fr.ses10doigts.toolkitbridge.service.tool;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;

import java.util.Map;
import java.util.Set;

public interface ToolHandler {
    String name();
    String description();
    Map<String, Object> parametersSchema();
    default ToolKind kind() {
        return ToolKind.NATIVE;
    }
    default ToolCategory category() {
        return ToolCategory.INTERNAL;
    }
    default Set<ToolCapability> capabilities() {
        return Set.of();
    }
    default ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ_ONLY;
    }
    default ToolDescriptor descriptor() {
        return new ToolDescriptor(
                name(),
                kind(),
                category(),
                description(),
                parametersSchema(),
                capabilities(),
                riskLevel()
        );
    }
    ToolExecutionResult execute(Map<String, Object> arguments) throws Exception;
}
