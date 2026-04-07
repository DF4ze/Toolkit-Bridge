package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InMemoryHumanInterventionServiceTest {

    @Test
    void opensPendingRequestAndNotifiesObservers() {
        RecordingNotifier notifier = new RecordingNotifier();
        InMemoryHumanInterventionService service = new InMemoryHumanInterventionService(List.of(notifier));

        HumanInterventionRequest request = service.open(new HumanInterventionDraft(
                "trace-1",
                "agent-1",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                "Review a sensitive tool execution",
                "Potential production impact",
                Map.of("toolName", "deploy")
        ));

        assertThat(request.status()).isEqualTo(HumanInterventionStatus.PENDING);
        assertThat(service.findPending()).containsExactly(request);
        assertThat(notifier.opened).containsExactly(request);
    }

    @Test
    void recordsDecisionWithoutCouplingToTelegramHandlers() {
        RecordingNotifier notifier = new RecordingNotifier();
        InMemoryHumanInterventionService service = new InMemoryHumanInterventionService(List.of(notifier));
        HumanInterventionRequest request = service.open(new HumanInterventionDraft(
                "trace-2",
                "agent-2",
                AgentSensitiveAction.DELEGATION,
                HumanInterventionKind.REVIEW,
                "Review delegation target",
                null,
                Map.of()
        ));

        HumanInterventionDecision decision = new HumanInterventionDecision(
                request.requestId(),
                HumanInterventionStatus.APPROVED,
                Instant.now(),
                "operator-1",
                "telegram",
                "Looks safe",
                Map.of()
        );

        HumanInterventionRequest updated = service.recordDecision(decision).orElseThrow();

        assertThat(updated.status()).isEqualTo(HumanInterventionStatus.APPROVED);
        assertThat(service.findPending()).isEmpty();
        assertThat(notifier.decisions).containsExactly(decision);
    }

    @Test
    void keepsRequestWhenNotifierFails() {
        InMemoryHumanInterventionService service = new InMemoryHumanInterventionService(List.of(new FailingNotifier()));

        HumanInterventionDraft draft = new HumanInterventionDraft(
                "trace-3",
                "agent-3",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                "Review a risky tool execution",
                null,
                Map.of()
        );

        assertThatCode(() -> service.open(draft)).doesNotThrowAnyException();
        assertThat(service.findPending()).hasSize(1);
    }

    private static class RecordingNotifier implements HumanInterventionNotifier {
        private final List<HumanInterventionRequest> opened = new ArrayList<>();
        private final List<HumanInterventionDecision> decisions = new ArrayList<>();

        @Override
        public void onRequestOpened(HumanInterventionRequest request) {
            opened.add(request);
        }

        @Override
        public void onDecisionRecorded(HumanInterventionRequest request, HumanInterventionDecision decision) {
            decisions.add(decision);
        }
    }

    private static class FailingNotifier implements HumanInterventionNotifier {
        @Override
        public void onRequestOpened(HumanInterventionRequest request) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void onDecisionRecorded(HumanInterventionRequest request, HumanInterventionDecision decision) {
            throw new IllegalStateException("boom");
        }
    }
}
