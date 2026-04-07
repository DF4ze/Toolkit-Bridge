package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

public interface HumanInterventionNotifier {

    void onRequestOpened(HumanInterventionRequest request);

    void onDecisionRecorded(HumanInterventionRequest request, HumanInterventionDecision decision);
}
