package fr.ses10doigts.toolkitbridge.exception;

public class AgentAlreadyExistsException extends RuntimeException {

    public AgentAlreadyExistsException(String agentId) {
        super("An agent already exists with id: " + agentId);
    }
}
