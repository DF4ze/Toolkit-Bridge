package fr.ses10doigts.toolkitbridge.model.dto.auth;

public record AgentProvisioningResult(
        String agentIdent,
        String apiKey
) {
}