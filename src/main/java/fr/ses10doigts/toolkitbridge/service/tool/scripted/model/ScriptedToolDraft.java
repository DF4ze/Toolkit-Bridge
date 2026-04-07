package fr.ses10doigts.toolkitbridge.service.tool.scripted.model;

import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;

import java.util.Map;
import java.util.Set;

public record ScriptedToolDraft(
        String name,
        String description,
        ToolCategory category,
        Map<String, Object> parametersSchema,
        Set<ToolCapability> capabilities,
        ToolRiskLevel riskLevel,
        String runtimeType,
        String scriptContent,
        Integer version,
        ScriptedToolLifecycleState state,
        ScriptedToolActivationStatus activationStatus,
        ScriptedToolOriginType originType,
        String originRef,
        String createdByAgentId,
        ScriptedToolRiskClass riskClass,
        ScriptedToolValidationMode validationMode,
        ScriptedToolValidationStatus validationStatus
) {
}
