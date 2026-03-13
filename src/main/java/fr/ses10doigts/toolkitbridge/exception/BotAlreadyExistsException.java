package fr.ses10doigts.toolkitbridge.exception;

public class BotAlreadyExistsException extends RuntimeException {

    public BotAlreadyExistsException(String botIdent) {
        super("A bot already exists with ident: " + botIdent);
    }
}