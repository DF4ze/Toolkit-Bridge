package fr.ses10doigts.toolkitbridge.service.agent.debate.service;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageBus;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageDispatchResult;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageDispatchStatus;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.ArtifactReviewRequest;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.ArtifactReviewResult;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateContext;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateMessageCommand;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateStage;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateTranscriptEntry;
import fr.ses10doigts.toolkitbridge.service.agent.process.ExternalProcessService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.TextTemplateProcess;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArtifactReviewCollaborationService {

    static final String ARTIFACT_REVIEW_SYNTHESIS_PROCESS_ID = "artifact-review-synthesis";

    private final DebateMessageFactory debateMessageFactory;
    private final AgentMessageBus agentMessageBus;
    private final ArtifactService artifactService;
    private final ExternalProcessService externalProcessService;
    private final AgentTraceService agentTraceService;

    public ArtifactReviewCollaborationService(DebateMessageFactory debateMessageFactory,
                                              AgentMessageBus agentMessageBus,
                                              ArtifactService artifactService,
                                              ExternalProcessService externalProcessService,
                                              AgentTraceService agentTraceService) {
        this.debateMessageFactory = debateMessageFactory;
        this.agentMessageBus = agentMessageBus;
        this.artifactService = artifactService;
        this.externalProcessService = externalProcessService;
        this.agentTraceService = agentTraceService;
    }

    public ArtifactReviewResult reviewArtifact(ArtifactReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        Artifact artifact = request.artifact();
        DebateContext questionContext = new DebateContext(
                null,
                artifact.taskId(),
                DebateStage.QUESTION,
                null,
                null,
                artifact.artifactId(),
                request.requestingAgentId()
        );

        AgentMessage questionMessage = debateMessageFactory.create(new DebateMessageCommand(
                request.requestingAgentId(),
                AgentRecipient.forRole(request.reviewerRole()),
                questionContext,
                request.reviewQuestion(),
                request.channelType(),
                request.channelUserId(),
                request.channelConversationId(),
                request.projectId(),
                request.context(),
                List.of(artifact.toReference())
        ));

        DebateContext normalizedQuestionContext = debateMessageFactory.extract(questionMessage)
                .orElseThrow(() -> new IllegalStateException("Question message should contain debate context"));
        List<DebateTranscriptEntry> transcript = new ArrayList<>();
        transcript.add(new DebateTranscriptEntry(
                questionMessage.messageId(),
                DebateStage.QUESTION,
                request.requestingAgentId(),
                questionMessage.payload().text(),
                questionMessage.timestamp(),
                null
        ));

        traceDebate(artifact.taskId(), request.requestingAgentId(), questionMessage.messageId(), DebateStage.QUESTION, Map.of(
                "debateId", normalizedQuestionContext.debateId(),
                "artifactId", artifact.artifactId(),
                "reviewerRole", request.reviewerRole().name()
        ));

        AgentMessageDispatchResult dispatchResult = agentMessageBus.dispatch(questionMessage);
        if (dispatchResult.status() != AgentMessageDispatchStatus.DELIVERED || dispatchResult.response() == null) {
            LinkedHashMap<String, Object> failureAttributes = new LinkedHashMap<>();
            failureAttributes.put("debateId", normalizedQuestionContext.debateId());
            failureAttributes.put("artifactId", artifact.artifactId());
            failureAttributes.put("status", dispatchResult.status().name());
            if (dispatchResult.details() != null) {
                failureAttributes.put("details", dispatchResult.details());
            }
            traceDebate(artifact.taskId(), request.requestingAgentId(), questionMessage.messageId(), DebateStage.CRITIQUE, failureAttributes);
            return new ArtifactReviewResult(
                    normalizedQuestionContext.debateId(),
                    dispatchResult.status(),
                    artifact,
                    dispatchResult.resolvedAgentId(),
                    null,
                    List.copyOf(transcript),
                    dispatchResult.details()
            );
        }

        String reviewerAgentId = dispatchResult.resolvedAgentId() == null
                ? request.reviewerRole().name().toLowerCase()
                : dispatchResult.resolvedAgentId();
        DebateTranscriptEntry critiqueEntry = new DebateTranscriptEntry(
                dispatchResult.messageId(),
                DebateStage.CRITIQUE,
                reviewerAgentId,
                dispatchResult.response().message(),
                Instant.now(),
                questionMessage.messageId()
        );
        transcript.add(critiqueEntry);

        traceDebate(artifact.taskId(), reviewerAgentId, critiqueEntry.messageId(), DebateStage.CRITIQUE, Map.of(
                "debateId", normalizedQuestionContext.debateId(),
                "artifactId", artifact.artifactId(),
                "status", dispatchResult.status().name()
        ));

        String synthesisText = buildSynthesisText(artifact, request.reviewQuestion(), request.reviewerRole(), reviewerAgentId, transcript);
        Artifact summaryArtifact = artifactService.create(new ArtifactDraft(
                ArtifactType.SUMMARY,
                artifact.taskId(),
                request.requestingAgentId(),
                "Artifact review - " + safeTitle(artifact),
                buildReviewMetadata(normalizedQuestionContext.debateId(), artifact, request.reviewerRole(), reviewerAgentId, questionMessage, critiqueEntry),
                synthesisText,
                "text/markdown",
                "artifact-review-" + artifact.artifactId() + ".md"
        ));

        DebateTranscriptEntry synthesisEntry = new DebateTranscriptEntry(
                summaryArtifact.artifactId(),
                DebateStage.SYNTHESIS,
                request.requestingAgentId(),
                synthesisText,
                Instant.now(),
                critiqueEntry.messageId()
        );
        transcript.add(synthesisEntry);

        traceDebate(artifact.taskId(), request.requestingAgentId(), synthesisEntry.messageId(), DebateStage.SYNTHESIS, Map.of(
                "debateId", normalizedQuestionContext.debateId(),
                "artifactId", artifact.artifactId(),
                "summaryArtifactId", summaryArtifact.artifactId()
        ));

        return new ArtifactReviewResult(
                normalizedQuestionContext.debateId(),
                dispatchResult.status(),
                artifact,
                reviewerAgentId,
                summaryArtifact,
                List.copyOf(transcript),
                dispatchResult.details()
        );
    }

    private Map<String, String> buildReviewMetadata(String debateId,
                                                    Artifact artifact,
                                                    AgentRole reviewerRole,
                                                    String reviewerAgentId,
                                                    AgentMessage questionMessage,
                                                    DebateTranscriptEntry critiqueEntry) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("debateId", debateId);
        metadata.put("reviewedArtifactId", artifact.artifactId());
        metadata.put("reviewedArtifactType", artifact.type().key());
        metadata.put("producerAgentId", artifact.producerAgentId());
        metadata.put("reviewerRole", reviewerRole.name());
        metadata.put("reviewerAgentId", reviewerAgentId);
        metadata.put("reviewQuestion", questionMessage.payload().text());
        metadata.put("questionMessageId", questionMessage.messageId());
        metadata.put("critiqueMessageId", critiqueEntry.messageId());
        return Map.copyOf(metadata);
    }

    private String buildSynthesisText(Artifact artifact,
                                      String reviewQuestion,
                                      AgentRole reviewerRole,
                                      String reviewerAgentId,
                                      List<DebateTranscriptEntry> transcript) {
        TextTemplateProcess template = externalProcessService.loadTypedContent(
                ARTIFACT_REVIEW_SYNTHESIS_PROCESS_ID,
                TextTemplateProcess.class
        );
        String critique = transcript.stream()
                .filter(entry -> entry.stage() == DebateStage.CRITIQUE)
                .map(DebateTranscriptEntry::text)
                .findFirst()
                .orElse("No critique available.");

        return externalProcessService.renderTemplate(template.template(), Map.of(
                "artifactId", artifact.artifactId(),
                "artifactType", artifact.type().key(),
                "producerAgentId", artifact.producerAgentId(),
                "reviewerRole", reviewerRole.name(),
                "reviewerAgentId", reviewerAgentId,
                "reviewQuestion", reviewQuestion,
                "critique", critique
        )).trim();
    }

    private String safeTitle(Artifact artifact) {
        if (artifact.title() == null || artifact.title().isBlank()) {
            return artifact.artifactId();
        }
        return artifact.title();
    }

    private void traceDebate(String taskId,
                             String agentId,
                             String messageId,
                             DebateStage stage,
                             Map<String, Object> attributes) {
        LinkedHashMap<String, Object> traceAttributes = new LinkedHashMap<>();
        traceAttributes.put("stage", stage.name());
        if (attributes != null) {
            traceAttributes.putAll(attributes);
        }
        agentTraceService.trace(
                AgentTraceEventType.DEBATE,
                "artifact_review_collaboration",
                new AgentTraceCorrelation(
                        attributes == null ? null : stringValue(attributes.get("debateId")),
                        taskId,
                        agentId,
                        messageId
                ),
                Map.copyOf(traceAttributes)
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
