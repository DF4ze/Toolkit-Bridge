package fr.ses10doigts.toolkitbridge.exception;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(String botIdent) {
        super("Bot not found: " + botIdent);
    }
}