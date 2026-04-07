package fr.ses10doigts.toolkitbridge.service.tool.scripted.service;

import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolActivationStatus;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolLifecycleState;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolMetadata;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolReviewerRole;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolRiskClass;
import fr.ses10doigts.toolkitbridge.service.tool.scripted.model.ScriptedToolValidationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultScriptedToolGovernanceServiceTest {

    private final DefaultScriptedToolGovernanceService service =
            new DefaultScriptedToolGovernanceService(new DefaultScriptedToolGovernanceProfileResolver());

    @Test
    void recommendsMultiAgentReviewForHighRiskClasses() {
        ScriptedToolMetadata metadata = new ScriptedToolMetadata();
        metadata.setRiskClass(ScriptedToolRiskClass.SYSTEM_MODIFICATION);

        assertThat(service.recommendedProfile(metadata).reviewerRoles())
                .contains(
                        ScriptedToolReviewerRole.DEVELOPER,
                        ScriptedToolReviewerRole.SECURITY,
                        ScriptedToolReviewerRole.HUMAN_APPROVER
                );
    }

    @Test
    void exposureReadinessRequiresExplicitActivationAndSatisfiedValidation() {
        ScriptedToolMetadata metadata = new ScriptedToolMetadata();
        metadata.setActivationStatus(ScriptedToolActivationStatus.INACTIVE);
        metadata.setValidationStatus(ScriptedToolValidationStatus.APPROVED);
        metadata.setState(ScriptedToolLifecycleState.READY);

        assertThat(service.evaluateExposureReadiness(metadata).eligibleForFutureExposure()).isFalse();

        metadata.setActivationStatus(ScriptedToolActivationStatus.ACTIVE);
        assertThat(service.evaluateExposureReadiness(metadata).eligibleForFutureExposure()).isTrue();

        metadata.setState(ScriptedToolLifecycleState.DRAFT);
        assertThat(service.evaluateExposureReadiness(metadata).eligibleForFutureExposure()).isFalse();
    }
}
