package fr.ses10doigts.toolkitbridge.service.tool.scripted.model;

import java.util.Set;

public record ScriptedToolGovernanceProfile(
        ScriptedToolRiskClass riskClass,
        ScriptedToolValidationMode validationMode,
        Set<ScriptedToolReviewerRole> reviewerRoles
) {
    public ScriptedToolGovernanceProfile {
        reviewerRoles = reviewerRoles == null ? Set.of() : Set.copyOf(reviewerRoles);
    }
}
