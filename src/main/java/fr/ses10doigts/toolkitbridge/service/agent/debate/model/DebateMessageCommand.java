package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DebateMessageCommand(
        String senderAgentId,
        AgentRecipient recipient,
        DebateContext debateContext,
        String text,
        String channelType,
        String channelUserId,
        String channelConversationId,
        String projectId,
        Map<String, Object> context,
        List<ArtifactReference> artifacts
) {

    public DebateMessageCommand {
        if (senderAgentId == null || senderAgentId.isBlank()) {
            throw new IllegalArgumentException("senderAgentId must not be blank");
        }
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(debateContext, "debateContext must not be null");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
