package fr.ses10doigts.toolkitbridge.service.tool.memory;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.memory.MemoryFactPayload;
import fr.ses10doigts.toolkitbridge.model.dto.tool.memory.MemoryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.memory.MemoryRulePayload;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolHandler;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;

import java.util.Set;

public abstract class AbstractMemoryToolHandler implements ToolHandler {

    @Override
    public ToolCategory category() {
        return ToolCategory.MEMORY;
    }

    @Override
    public Set<ToolCapability> capabilities() {
        return Set.of(defaultMemoryCapability());
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return defaultMemoryCapability() == ToolCapability.MEMORY_READ
                ? ToolRiskLevel.READ_ONLY
                : ToolRiskLevel.LOCAL_WRITE;
    }

    protected abstract ToolCapability defaultMemoryCapability();

    protected ToolExecutionResult successWithContext(String message, String operation, String context) {
        return ToolExecutionResult.builder()
                .error(false)
                .message(message)
                .memory(MemoryResponse.builder()
                        .operation(operation)
                        .context(context)
                        .build())
                .build();
    }

    protected ToolExecutionResult successWithFact(String message, String operation, MemoryEntry entry) {
        return ToolExecutionResult.builder()
                .error(false)
                .message(message)
                .memory(MemoryResponse.builder()
                        .operation(operation)
                        .fact(MemoryFactPayload.builder()
                                .id(entry.getId())
                                .scope(enumName(entry.getScope()))
                                .scopeId(entry.getScopeId())
                                .type(enumName(entry.getType()))
                                .content(entry.getContent())
                                .importance(entry.getImportance())
                                .status(enumName(entry.getStatus()))
                                .writeMode(enumName(entry.getWriteMode()))
                                .tags(entry.getTags())
                                .build())
                        .build())
                .build();
    }

    protected ToolExecutionResult successWithRule(String message, String operation, RuleEntry entry) {
        return ToolExecutionResult.builder()
                .error(false)
                .message(message)
                .memory(MemoryResponse.builder()
                        .operation(operation)
                        .rule(MemoryRulePayload.builder()
                                .id(entry.getId())
                                .scope(enumName(entry.getScope()))
                                .scopeId(entry.getScopeId())
                                .title(entry.getTitle())
                                .content(entry.getContent())
                                .priority(enumName(entry.getPriority()))
                                .status(enumName(entry.getStatus()))
                                .writeMode(enumName(entry.getWriteMode()))
                                .build())
                        .build())
                .build();
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
