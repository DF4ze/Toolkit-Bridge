package fr.ses10doigts.toolkitbridge.exception;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String agentId) {
        super("Agent not found: " + agentId);
    }
}
