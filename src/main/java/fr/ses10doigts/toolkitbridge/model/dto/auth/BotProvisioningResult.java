package fr.ses10doigts.toolkitbridge.model.dto.auth;

public record BotProvisioningResult(
        String botIdent,
        String apiKey
) {
}