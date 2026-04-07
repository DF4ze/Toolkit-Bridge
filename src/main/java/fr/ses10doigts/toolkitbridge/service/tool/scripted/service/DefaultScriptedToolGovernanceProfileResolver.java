package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolGovernanceProfile;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolReviewerRole;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolRiskClass;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolValidationMode;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DefaultScriptedToolGovernanceProfileResolver implements ScriptedToolGovernanceProfileResolver {

    @Override
    public ScriptedToolGovernanceProfile resolve(ScriptedToolRiskClass riskClass) {
        ScriptedToolRiskClass effectiveRiskClass = riskClass == null ? ScriptedToolRiskClass.READ_ONLY : riskClass;
        return switch (effectiveRiskClass) {
            case READ_ONLY -> new ScriptedToolGovernanceProfile(
                    effectiveRiskClass,
                    ScriptedToolValidationMode.AUTOMATED_CHECKS,
                    Set.of(ScriptedToolReviewerRole.DEVELOPER)
            );
            case WRITE -> new ScriptedToolGovernanceProfile(
                    effectiveRiskClass,
                    ScriptedToolValidationMode.HUMAN_REVIEW,
                    Set.of(ScriptedToolReviewerRole.DEVELOPER, ScriptedToolReviewerRole.HUMAN_APPROVER)
            );
            case EXFILTRATION, SYSTEM_MODIFICATION -> new ScriptedToolGovernanceProfile(
                    effectiveRiskClass,
                    ScriptedToolValidationMode.MULTI_AGENT_REVIEW,
                    Set.of(
                            ScriptedToolReviewerRole.DEVELOPER,
                            ScriptedToolReviewerRole.SECURITY,
                            ScriptedToolReviewerRole.HUMAN_APPROVER
                    )
            );
        };
    }
}
