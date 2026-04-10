package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersistentHumanInterventionServiceTest {

    private HumanInterventionRepository repository;
    private HumanInterventionNotifier notifier;
    private PersistentHumanInterventionService service;

    @BeforeEach
    void setUp() {
        repository = mock(HumanInterventionRepository.class);
        notifier = mock(HumanInterventionNotifier.class);
        service = new PersistentHumanInterventionService(
                repository,
                new HumanInterventionEntityMapper(new tools.jackson.databind.ObjectMapper()),
                List.of(notifier)
        );
    }

    @Test
    void openPersistsAndNotifies() {
        HumanInterventionEntity saved = pendingEntity("req-1", Instant.parse("2026-04-10T10:00:00Z"));
        when(repository.save(any(HumanInterventionEntity.class))).thenReturn(saved);

        HumanInterventionRequest opened = service.open(new HumanInterventionDraft(
                "trace-1",
                "agent-1",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                "Approve a sensitive tool",
                "detail",
                Map.of("toolName", "deploy", "apiToken", "redacted")
        ));

        assertThat(opened.requestId()).isEqualTo("req-1");
        assertThat(opened.status()).isEqualTo(HumanInterventionStatus.PENDING);
        assertThat(opened.metadata()).containsEntry("toolName", "deploy");
        assertThat(opened.metadata()).doesNotContainKey("apiToken");
        verify(notifier).onRequestOpened(opened);
    }

    @Test
    void findByIdReturnsEmptyForBlankId() {
        Optional<HumanInterventionRequest> result = service.findById(" ");

        assertThat(result).isEmpty();
        verify(repository, never()).findByRequestId(any());
    }

    @Test
    void findPendingMapsRepositoryResults() {
        when(repository.findByStatusOrderByCreatedAtAsc(HumanInterventionStatus.PENDING))
                .thenReturn(List.of(
                        pendingEntity("req-1", Instant.parse("2026-04-10T10:00:00Z")),
                        pendingEntity("req-2", Instant.parse("2026-04-10T11:00:00Z"))
                ));

        List<HumanInterventionRequest> pending = service.findPending();

        assertThat(pending).hasSize(2);
        assertThat(pending).extracting(HumanInterventionRequest::requestId).containsExactly("req-1", "req-2");
    }

    @Test
    void recordDecisionReturnsEmptyWhenAlreadyHandledOrUnknown() {
        when(repository.updateDecisionIfPending(
                eq("req-1"),
                eq(HumanInterventionStatus.PENDING),
                eq(HumanInterventionStatus.APPROVED),
                eq(Instant.parse("2026-04-10T12:00:00Z")),
                eq("operator-1"),
                eq("telegram"),
                eq("ok"),
                isNull()
        )).thenReturn(0);

        Optional<HumanInterventionRequest> result = service.recordDecision(new HumanInterventionDecision(
                "req-1",
                HumanInterventionStatus.APPROVED,
                Instant.parse("2026-04-10T12:00:00Z"),
                "operator-1",
                "telegram",
                "ok",
                Map.of()
        ));

        assertThat(result).isEmpty();
        verify(notifier, never()).onDecisionRecorded(any(), any());
    }

    @Test
    void recordDecisionUpdatesAndNotifies() {
        HumanInterventionEntity decided = pendingEntity("req-1", Instant.parse("2026-04-10T10:00:00Z"));
        decided.setStatus(HumanInterventionStatus.APPROVED);
        decided.setDecisionStatus(HumanInterventionStatus.APPROVED);
        decided.setDecisionDecidedAt(Instant.parse("2026-04-10T12:00:00Z"));
        when(repository.updateDecisionIfPending(
                eq("req-1"),
                eq(HumanInterventionStatus.PENDING),
                eq(HumanInterventionStatus.APPROVED),
                eq(Instant.parse("2026-04-10T12:00:00Z")),
                eq("operator-1"),
                eq("telegram"),
                eq("ok"),
                any(String.class)
        )).thenReturn(1);
        when(repository.findByRequestId("req-1")).thenReturn(Optional.of(decided));

        HumanInterventionDecision decision = new HumanInterventionDecision(
                "req-1",
                HumanInterventionStatus.APPROVED,
                Instant.parse("2026-04-10T12:00:00Z"),
                "operator-1",
                "telegram",
                "ok",
                Map.of("reason", "safe")
        );

        Optional<HumanInterventionRequest> result = service.recordDecision(decision);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().status()).isEqualTo(HumanInterventionStatus.APPROVED);
        verify(repository).updateDecisionIfPending(
                eq("req-1"),
                eq(HumanInterventionStatus.PENDING),
                eq(HumanInterventionStatus.APPROVED),
                eq(Instant.parse("2026-04-10T12:00:00Z")),
                eq("operator-1"),
                eq("telegram"),
                eq("ok"),
                argThat(json -> json != null && json.contains("\"reason\""))
        );
        verify(notifier).onDecisionRecorded(result.orElseThrow(), decision);
    }

    private HumanInterventionEntity pendingEntity(String requestId, Instant createdAt) {
        HumanInterventionEntity entity = new HumanInterventionEntity();
        entity.setRequestId(requestId);
        entity.setCreatedAt(createdAt);
        entity.setTraceId("trace-1");
        entity.setAgentId("agent-1");
        entity.setSensitiveAction(AgentSensitiveAction.TOOL_EXECUTION);
        entity.setKind(HumanInterventionKind.APPROVAL);
        entity.setStatus(HumanInterventionStatus.PENDING);
        entity.setSummary("summary");
        entity.setDetail("detail");
        entity.setRequestMetadataJson("{\"toolName\":\"deploy\"}");
        return entity;
    }
}
