package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@Slf4j
public class PersistentHumanInterventionService implements HumanInterventionService {

    private final HumanInterventionRepository repository;
    private final HumanInterventionEntityMapper mapper;
    private final List<HumanInterventionNotifier> notifiers;

    public PersistentHumanInterventionService(HumanInterventionRepository repository,
                                              HumanInterventionEntityMapper mapper,
                                              List<HumanInterventionNotifier> notifiers) {
        this.repository = repository;
        this.mapper = mapper;
        this.notifiers = notifiers == null ? List.of() : List.copyOf(notifiers);
    }

    @Override
    @Transactional
    public HumanInterventionRequest open(HumanInterventionDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }

        HumanInterventionEntity entity = mapper.toEntityForOpen(
                UUID.randomUUID().toString(),
                Instant.now(),
                draft
        );
        HumanInterventionEntity saved = repository.save(entity);
        HumanInterventionRequest request = mapper.toDomainRequest(saved);
        notifyRequestOpened(request);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HumanInterventionRequest> findById(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByRequestId(requestId.trim()).map(mapper::toDomainRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HumanInterventionRequest> findPending() {
        return repository.findByStatusOrderByCreatedAtAsc(HumanInterventionStatus.PENDING)
                .stream()
                .map(mapper::toDomainRequest)
                .toList();
    }

    @Override
    @Transactional
    public Optional<HumanInterventionRequest> recordDecision(HumanInterventionDecision decision) {
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null");
        }

        int updated = repository.updateDecisionIfPending(
                decision.requestId(),
                HumanInterventionStatus.PENDING,
                decision.status(),
                decision.decidedAt(),
                decision.actorId(),
                decision.channel(),
                decision.comment(),
                mapper.toDecisionMetadataJson(decision)
        );

        if (updated == 0) {
            return Optional.empty();
        }

        return repository.findByRequestId(decision.requestId())
                .map(mapper::toDomainRequest)
                .map(updatedRequest -> {
                    notifyDecisionRecorded(updatedRequest, decision);
                    return updatedRequest;
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
