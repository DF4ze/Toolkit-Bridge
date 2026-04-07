package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task;

import fr.ses10doigts.toolkitbridge.persistence.model.EphemeralObject;

public record TaskPrompt(
        String systemPrompt,
        String userPrompt
) implements EphemeralObject {
}
