package fr.ses10doigts.toolkitbridge.service.tool.scripted.model;

public record ScriptedToolExposureReadiness(
        boolean explicitlyActivated,
        boolean validationSatisfied,
        boolean eligibleForFutureExposure
) {
}
