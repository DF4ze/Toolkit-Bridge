package fr.ses10doigts.toolkitbridge.model.dto.admin.agent;

import java.time.Instant;
import java.util.UUID;

public record AgentAdminDetailResponse(
        UUID accountId,
        String agentId,
        boolean enabled,
        Instant createdAt
) {
}
