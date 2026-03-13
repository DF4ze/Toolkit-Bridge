package fr.ses10doigts.toolkitbridge.exception;

public class BotNotFoundException extends RuntimeException {

    public BotNotFoundException(String botIdent) {
        super("Bot not found: " + botIdent);
    }
}