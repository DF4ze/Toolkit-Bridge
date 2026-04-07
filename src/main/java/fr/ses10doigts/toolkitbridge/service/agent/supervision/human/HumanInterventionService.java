package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import java.util.List;
import java.util.Optional;

public interface HumanInterventionService {

    HumanInterventionRequest open(HumanInterventionDraft draft);

    Optional<HumanInterventionRequest> findById(String requestId);

    List<HumanInterventionRequest> findPending();

    Optional<HumanInterventionRequest> recordDecision(HumanInterventionDecision decision);
}
