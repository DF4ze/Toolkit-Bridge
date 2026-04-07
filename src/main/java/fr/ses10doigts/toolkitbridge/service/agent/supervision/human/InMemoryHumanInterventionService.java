package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InMemoryHumanInterventionService implements HumanInterventionService {

    private final Map<String, HumanInterventionRequest> requests = new ConcurrentHashMap<>();
    private final List<HumanInterventionNotifier> notifiers;

    public InMemoryHumanInterventionService(List<HumanInterventionNotifier> notifiers) {
        this.notifiers = notifiers == null ? List.of() : List.copyOf(notifiers);
    }

    @Override
    public HumanInterventionRequest open(HumanInterventionDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }

        HumanInterventionRequest request = new HumanInterventionRequest(
                UUID.randomUUID().toString(),
                Instant.now(),
                draft.traceId(),
                draft.agentId(),
                draft.sensitiveAction(),
                draft.kind(),
                HumanInterventionStatus.PENDING,
                draft.summary(),
                draft.detail(),
                draft.metadata()
        );
        requests.put(request.requestId(), request);
        notifyRequestOpened(request);
        return request;
    }

    @Override
    public Optional<HumanInterventionRequest> findById(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(requests.get(requestId.trim()));
    }

    @Override
    public List<HumanInterventionRequest> findPending() {
        List<HumanInterventionRequest> pending = new ArrayList<>();
        for (HumanInterventionRequest request : requests.values()) {
            if (request.status() == HumanInterventionStatus.PENDING) {
                pending.add(request);
            }
        }
        pending.sort(Comparator.comparing(HumanInterventionRequest::createdAt));
        return List.copyOf(pending);
    }

    @Override
    public Optional<HumanInterventionRequest> recordDecision(HumanInterventionDecision decision) {
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null");
        }

        AtomicReference<HumanInterventionRequest> updatedReference = new AtomicReference<>();
        requests.computeIfPresent(decision.requestId(), (requestId, request) -> {
            if (request.status() != HumanInterventionStatus.PENDING) {
                return request;
            }
            HumanInterventionRequest updated = request.withStatus(decision.status());
            updatedReference.set(updated);
            return updated;
        });

        return Optional.ofNullable(updatedReference.get())
                .map(updated -> {
                    notifyDecisionRecorded(updated, decision);
                    return updated;
                });
    }

    private void notifyRequestOpened(HumanInterventionRequest request) {
        for (HumanInterventionNotifier notifier : notifiers) {
            try {
                notifier.onRequestOpened(request);
            } catch (RuntimeException e) {
                log.warn("Human intervention notifier failed on request opening requestId={}", request.requestId(), e);
            }
        }
    }

    private void notifyDecisionRecorded(HumanInterventionRequest request, HumanInterventionDecision decision) {
        for (HumanInterventionNotifier notifier : notifiers) {
            try {
                notifier.onDecisionRecorded(request, decision);
            } catch (RuntimeException e) {
                log.warn("Human intervention notifier failed on decision recording requestId={}", request.requestId(), e);
            }
        }
    }
}
