package fr.ses10doigts.toolkitbridge.model.dto.auth;

import java.util.UUID;

public record AuthenticatedAgent(
        UUID id,
        String agentIdent
) {
}