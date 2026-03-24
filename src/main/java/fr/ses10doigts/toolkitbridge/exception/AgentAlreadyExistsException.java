package fr.ses10doigts.toolkitbridge.exception;

public class AgentAlreadyExistsException extends RuntimeException {

    public AgentAlreadyExistsException(String botIdent) {
        super("A bot already exists with ident: " + botIdent);
    }
}