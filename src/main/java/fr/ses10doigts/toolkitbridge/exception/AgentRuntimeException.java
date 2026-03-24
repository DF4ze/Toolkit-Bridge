package fr.ses10doigts.toolkitbridge.exception;

public class AgentRuntimeException extends RuntimeException {

    public AgentRuntimeException(String message) {
        super(message);
    }

    public AgentRuntimeException(String message, Exception e) {
        super(message, e);
    }
}