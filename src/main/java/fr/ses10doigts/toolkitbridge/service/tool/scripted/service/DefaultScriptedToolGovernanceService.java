package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolActivationStatus;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolExposureReadiness;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolGovernanceProfile;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolLifecycleState;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolMetadata;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolValidationStatus;
import org.springframework.stereotype.Service;

@Service
public class DefaultScriptedToolGovernanceService implements ScriptedToolGovernanceService {

    private final ScriptedToolGovernanceProfileResolver profileResolver;

    public DefaultScriptedToolGovernanceService(ScriptedToolGovernanceProfileResolver profileResolver) {
        this.profileResolver = profileResolver;
    }

    @Override
    public ScriptedToolGovernanceProfile recommendedProfile(ScriptedToolMetadata metadata) {
        return profileResolver.resolve(metadata == null ? null : metadata.getRiskClass());
    }

    @Override
    public ScriptedToolExposureReadiness evaluateExposureReadiness(ScriptedToolMetadata metadata) {
        if (metadata == null) {
            return new ScriptedToolExposureReadiness(false, false, false);
        }

        boolean explicitlyActivated = metadata.getActivationStatus() == ScriptedToolActivationStatus.ACTIVE;
        boolean validationSatisfied = metadata.getValidationStatus() == ScriptedToolValidationStatus.APPROVED
                || metadata.getValidationStatus() == ScriptedToolValidationStatus.NOT_REQUIRED;
        boolean lifecycleReady = metadata.getState() == ScriptedToolLifecycleState.READY;
        boolean eligibleForFutureExposure = explicitlyActivated
                && validationSatisfied
                && lifecycleReady;

        return new ScriptedToolExposureReadiness(
                explicitlyActivated,
                validationSatisfied,
                eligibleForFutureExposure
        );
    }
}
