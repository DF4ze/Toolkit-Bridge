package fr.ses10doigts.toolkitbridge.service.agent.improvement.model;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;

public record ImprovementProposal(
        ImprovementProposalMode mode,
        ImprovementObservation observation,
        ImprovementRecommendation recommendation,
        Artifact artifact
) {
}
