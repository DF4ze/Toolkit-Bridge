package fr.ses10doigts.toolkitbridge.model.dto.web.admin;

import java.time.Instant;

public final class AgentAdminPageView {

    private AgentAdminPageView() {
    }

    public record AgentItem(
            String agentId,
            String name,
            String orchestratorType,
            String role,
            String llmProvider,
            String model,
            String telegramBotId,
            String policyName,
            boolean toolsEnabled,
            String systemPrompt,
            Boolean accountEnabled,
            Instant accountCreatedAt
    ) {
    }
}
