package fr.ses10doigts.toolkitbridge.exception;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;

public class AgentPermissionDeniedException extends AgentException {

    public AgentPermissionDeniedException(String agentId, AgentSensitiveAction action, String detail) {
        super("Permission denied for agentId=%s action=%s detail=%s"
                .formatted(agentId, action == null ? "unknown" : action.name(), detail == null ? "n/a" : detail));
    }
}
