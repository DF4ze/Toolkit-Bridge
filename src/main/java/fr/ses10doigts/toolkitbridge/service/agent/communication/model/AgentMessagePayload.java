package fr.ses10doigts.toolkitbridge.service.agent.communication.model;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;

import java.util.List;
import java.util.Map;

public record AgentMessagePayload(
        String text,
        String channelType,
        String channelUserId,
        String channelConversationId,
        String projectId,
        Map<String, Object> context,
        List<ArtifactReference> artifacts
) {

    public AgentMessagePayload(String text,
                               String channelType,
                               String channelUserId,
                               String channelConversationId,
                               String projectId,
                               Map<String, Object> context) {
        this(text, channelType, channelUserId, channelConversationId, projectId, context, List.of());
    }

    public AgentMessagePayload {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
