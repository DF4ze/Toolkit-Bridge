package fr.ses10doigts.toolkitbridge.service.tool.scripted.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record StoredScriptedTool(
        Long id,
        String name,
        String description,
        ToolCategory category,
        Map<String, Object> parametersSchema,
        Set<ToolCapability> capabilities,
        ToolRiskLevel riskLevel,
        String runtimeType,
        int version,
        ScriptedToolLifecycleState state,
        ScriptedToolActivationStatus activationStatus,
        ScriptedToolOriginType originType,
        String originRef,
        String createdByAgentId,
        ScriptedToolRiskClass riskClass,
        ScriptedToolValidationMode validationMode,
        ScriptedToolValidationStatus validationStatus,
        String scriptPath,
        String scriptChecksum,
        Instant createdAt,
        Instant updatedAt
) implements DurableObject {

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.SCRIPTED_TOOL;
    }
}
