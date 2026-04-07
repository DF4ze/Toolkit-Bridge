package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;

import java.util.Map;
import java.util.Objects;

public record ArtifactReviewRequest(
        Artifact artifact,
        String requestingAgentId,
        AgentRole reviewerRole,
        String reviewQuestion,
        String channelType,
        String channelUserId,
        String channelConversationId,
        String projectId,
        Map<String, Object> context
) {

    public ArtifactReviewRequest {
        Objects.requireNonNull(artifact, "artifact must not be null");
        if (requestingAgentId == null || requestingAgentId.isBlank()) {
            throw new IllegalArgumentException("requestingAgentId must not be blank");
        }
        Objects.requireNonNull(reviewerRole, "reviewerRole must not be null");
        if (reviewQuestion == null || reviewQuestion.isBlank()) {
            throw new IllegalArgumentException("reviewQuestion must not be blank");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}
