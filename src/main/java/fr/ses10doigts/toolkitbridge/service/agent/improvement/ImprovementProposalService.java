package fr.ses10doigts.toolkitbridge.service.agent.improvement;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementObservation;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposal;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposalDraft;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementProposalMode;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementRecommendation;
import fr.ses10doigts.toolkitbridge.service.agent.improvement.model.ImprovementTargetType;
import fr.ses10doigts.toolkitbridge.service.agent.process.ExternalProcessService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.TextTemplateProcess;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ImprovementProposalService {

    static final String IMPROVEMENT_PROPOSAL_PROCESS_ID = "improvement-proposal-report";

    private final ArtifactService artifactService;
    private final ExternalProcessService externalProcessService;
    private final AgentTraceService agentTraceService;

    public ImprovementProposalService(ArtifactService artifactService,
                                      ExternalProcessService externalProcessService,
                                      AgentTraceService agentTraceService) {
        this.artifactService = artifactService;
        this.externalProcessService = externalProcessService;
        this.agentTraceService = agentTraceService;
    }

    public ImprovementProposal propose(ImprovementProposalDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }

        ImprovementObservation observation = draft.observation();
        ImprovementRecommendation recommendation = draft.recommendation();
        traceObservation(draft, observation, recommendation);

        String report = buildProposalReport(draft, observation, recommendation);
        Artifact artifact = artifactService.create(new ArtifactDraft(
                ArtifactType.PROPOSAL,
                draft.taskId(),
                draft.producerAgentId(),
                "Improvement proposal - " + observation.title(),
                buildMetadata(draft, observation, recommendation),
                report,
                "text/markdown",
                "improvement-proposal-" + draft.taskId() + ".md"
        ));

        traceProposal(draft, observation, recommendation, artifact);
        return new ImprovementProposal(
                ImprovementProposalMode.PROPOSAL_ONLY,
                observation,
                recommendation,
                artifact
        );
    }

    private Map<String, String> buildMetadata(ImprovementProposalDraft draft,
                                              ImprovementObservation observation,
                                              ImprovementRecommendation recommendation) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("mode", ImprovementProposalMode.PROPOSAL_ONLY.name());
        metadata.put("observationCategory", observation.category());
        metadata.put("recommendationTargetType", recommendation.targetType().name());
        metadata.put("recommendationTargetReference", safe(recommendation.targetReference()));
        metadata.put("traceId", draft.traceId());
        metadata.putAll(draft.metadata());
        return Map.copyOf(metadata);
    }

    private void traceObservation(ImprovementProposalDraft draft,
                                  ImprovementObservation observation,
                                  ImprovementRecommendation recommendation) {
        agentTraceService.trace(
                AgentTraceEventType.IMPROVEMENT_OBSERVATION,
                "improvement_proposal_service",
                new AgentTraceCorrelation(draft.traceId(), draft.taskId(), draft.producerAgentId(), null),
                Map.of(
                        "category", observation.category(),
                        "title", observation.title(),
                        "targetType", recommendation.targetType().name(),
                        "targetReference", safe(recommendation.targetReference()),
                        "mode", ImprovementProposalMode.PROPOSAL_ONLY.name()
                )
        );
    }

    private void traceProposal(ImprovementProposalDraft draft,
                               ImprovementObservation observation,
                               ImprovementRecommendation recommendation,
                               Artifact artifact) {
        agentTraceService.trace(
                AgentTraceEventType.IMPROVEMENT_PROPOSAL,
                "improvement_proposal_service",
                new AgentTraceCorrelation(draft.traceId(), draft.taskId(), draft.producerAgentId(), artifact.artifactId()),
                Map.of(
                        "artifactId", artifact.artifactId(),
                        "category", observation.category(),
                        "targetType", recommendation.targetType().name(),
                        "targetReference", safe(recommendation.targetReference()),
                        "mode", ImprovementProposalMode.PROPOSAL_ONLY.name()
                )
        );
    }

    private String buildProposalReport(ImprovementProposalDraft draft,
                                       ImprovementObservation observation,
                                       ImprovementRecommendation recommendation) {
        TextTemplateProcess template = externalProcessService.loadTypedContent(
                IMPROVEMENT_PROPOSAL_PROCESS_ID,
                TextTemplateProcess.class
        );
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        variables.put("mode", ImprovementProposalMode.PROPOSAL_ONLY.name());
        variables.put("taskId", draft.taskId());
        variables.put("traceId", draft.traceId());
        variables.put("producerAgentId", draft.producerAgentId());
        variables.put("observationCategory", observation.category());
        variables.put("observationTitle", observation.title());
        variables.put("observationDetail", observation.detail());
        variables.put("observationEvidence", formatEvidence(observation.evidence()));
        variables.put("targetType", recommendation.targetType().name());
        variables.put("targetReference", safe(recommendation.targetReference()));
        variables.put("recommendationSummary", recommendation.summary());
        variables.put("recommendationRationale", recommendation.rationale());
        variables.put("suggestedChange", recommendation.suggestedChange());
        variables.put("guardrailNote", guardrailNote(recommendation.targetType()));
        return externalProcessService.renderTemplate(template.template(), Map.copyOf(variables)).trim();
    }

    private String formatEvidence(Map<String, String> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "- none";
        }
        return evidence.entrySet().stream()
                .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- none");
    }

    private String guardrailNote(ImprovementTargetType targetType) {
        if (targetType == ImprovementTargetType.PROCESS) {
            return "This proposal may guide a future controlled process update, but it does not modify the active process.";
        }
        return "This proposal is advisory only and does not modify the running system.";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
