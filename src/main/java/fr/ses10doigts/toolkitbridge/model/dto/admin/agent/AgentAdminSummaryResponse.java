package fr.ses10doigts.toolkitbridge.model.dto.admin.agent;

import java.time.Instant;

public record AgentAdminSummaryResponse(
        String agentId,
        boolean enabled,
        Instant createdAt
) {
}
