package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolExposureReadiness;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolGovernanceProfile;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolMetadata;

public interface ScriptedToolGovernanceService {

    ScriptedToolGovernanceProfile recommendedProfile(ScriptedToolMetadata metadata);

    ScriptedToolExposureReadiness evaluateExposureReadiness(ScriptedToolMetadata metadata);
}
