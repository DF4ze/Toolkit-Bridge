package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolGovernanceProfile;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolRiskClass;

public interface ScriptedToolGovernanceProfileResolver {

    ScriptedToolGovernanceProfile resolve(ScriptedToolRiskClass riskClass);
}
