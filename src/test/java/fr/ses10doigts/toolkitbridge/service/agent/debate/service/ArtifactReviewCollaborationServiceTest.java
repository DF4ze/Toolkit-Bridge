package fr.ses10doigts.toolkitbridge.service.agent.debate.service;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageBus;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageDispatchResult;
import fr.ses10doigts.toolkitbridge.service.agent.communication.bus.AgentMessageDispatchStatus;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.ArtifactReviewRequest;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.ArtifactReviewResult;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactReviewCollaborationServiceTest {

    @Test
    void runsSimpleCrossReviewAndStoresSummaryArtifact() {
        DebateMessageFactory debateMessageFactory = new DebateMessageFactory();
        AgentMessageBus messageBus = mock(AgentMessageBus.class);
        ArtifactService artifactService = mock(ArtifactService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);

        Artifact sourceArtifact = new Artifact(
                "artifact-1",
                ArtifactType.PLAN,
                "task-1",
                "agent-producer",
                "Execution plan",
                Instant.parse("2026-01-10T00:00:00Z"),
                Instant.parse("2026-01-11T00:00:00Z"),
                Map.of(),
                new ArtifactContentPointer("workspace", "artifacts/plan/1.md", "text/markdown", 10)
        );
        Artifact summaryArtifact = new Artifact(
                "artifact-summary-1",
                ArtifactType.SUMMARY,
                "task-1",
                "agent-producer",
                "Artifact review - Execution plan",
                Instant.parse("2026-01-10T00:00:00Z"),
                Instant.parse("2026-01-11T00:00:00Z"),
                Map.of("reviewedArtifactId", "artifact-1"),
                new ArtifactContentPointer("workspace", "artifacts/summary/1.md", "text/markdown", 10)
        );

        when(messageBus.dispatch(any(AgentMessage.class))).thenReturn(AgentMessageDispatchResult.delivered(
                "message-1",
                "debate-1",
                "agent-critic",
                AgentResponse.success("The plan is solid but needs rollback criteria.")
        ));
        when(artifactService.create(any(ArtifactDraft.class))).thenReturn(summaryArtifact);

        ArtifactReviewCollaborationService service = new ArtifactReviewCollaborationService(
                debateMessageFactory,
                messageBus,
                artifactService,
                traceService
        );

        ArtifactReviewResult result = service.reviewArtifact(new ArtifactReviewRequest(
                sourceArtifact,
                "agent-producer",
                AgentRole.CRITIC,
                "Review this plan with a critical delivery perspective.",
                "internal",
                "agent-producer",
                "conv-1",
                "project-1",
                Map.of("origin", "phase15b")
        ));

        ArgumentCaptor<AgentMessage> messageCaptor = ArgumentCaptor.forClass(AgentMessage.class);
        ArgumentCaptor<ArtifactDraft> artifactDraftCaptor = ArgumentCaptor.forClass(ArtifactDraft.class);
        verify(messageBus).dispatch(messageCaptor.capture());
        verify(artifactService).create(artifactDraftCaptor.capture());

        AgentMessage dispatchedMessage = messageCaptor.getValue();
        ArtifactDraft summaryDraft = artifactDraftCaptor.getValue();

        assertThat(result.status()).isEqualTo(AgentMessageDispatchStatus.DELIVERED);
        assertThat(result.summaryArtifact()).isEqualTo(summaryArtifact);
        assertThat(result.reviewerAgentId()).isEqualTo("agent-critic");
        assertThat(result.transcript()).hasSize(3);
        assertThat(result.transcript().get(1).messageId()).isEqualTo("message-1");
        assertThat(result.transcript().get(1).text()).contains("rollback criteria");
        assertThat(dispatchedMessage.correlationId()).isEqualTo(result.debateId());
        assertThat(dispatchedMessage.payload().artifacts()).containsExactly(sourceArtifact.toReference());
        assertThat(dispatchedMessage.payload().context())
                .containsEntry("origin", "phase15b")
                .containsEntry("debateSubjectArtifactId", "artifact-1");
        assertThat(summaryDraft.metadata())
                .containsEntry("reviewedArtifactId", "artifact-1")
                .containsEntry("questionMessageId", dispatchedMessage.messageId())
                .containsEntry("critiqueMessageId", "message-1");
        assertThat(summaryDraft.content()).contains("Review question");
    }

    @Test
    void returnsTraceableFailureWhenReviewCannotBeDelivered() {
        DebateMessageFactory debateMessageFactory = new DebateMessageFactory();
        AgentMessageBus messageBus = mock(AgentMessageBus.class);
        ArtifactService artifactService = mock(ArtifactService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);

        Artifact sourceArtifact = new Artifact(
                "artifact-1",
                ArtifactType.PLAN,
                "task-1",
                "agent-producer",
                "Execution plan",
                Instant.parse("2026-01-10T00:00:00Z"),
                Instant.parse("2026-01-11T00:00:00Z"),
                Map.of(),
                new ArtifactContentPointer("workspace", "artifacts/plan/1.md", "text/markdown", 10)
        );

        when(messageBus.dispatch(any(AgentMessage.class))).thenReturn(AgentMessageDispatchResult.unroutable(
                "message-1",
                "debate-1",
                "No reviewer available"
        ));

        ArtifactReviewCollaborationService service = new ArtifactReviewCollaborationService(
                debateMessageFactory,
                messageBus,
                artifactService,
                traceService
        );

        ArtifactReviewResult result = service.reviewArtifact(new ArtifactReviewRequest(
                sourceArtifact,
                "agent-producer",
                AgentRole.CRITIC,
                "Review this plan with a critical delivery perspective.",
                "internal",
                "agent-producer",
                "conv-1",
                "project-1",
                Map.of()
        ));

        assertThat(result.status()).isEqualTo(AgentMessageDispatchStatus.UNROUTABLE);
        assertThat(result.summaryArtifact()).isNull();
        assertThat(result.details()).contains("reviewer");
        assertThat(result.transcript()).hasSize(1);
        verify(artifactService, never()).create(any(ArtifactDraft.class));
    }
}
