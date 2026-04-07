package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageDispatchStatus;

import java.util.List;

public record ArtifactReviewResult(
        String debateId,
        AgentMessageDispatchStatus status,
        Artifact reviewedArtifact,
        String reviewerAgentId,
        Artifact summaryArtifact,
        List<DebateTranscriptEntry> transcript,
        String details
) {
}
