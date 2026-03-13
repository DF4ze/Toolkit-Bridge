package fr.ses10doigts.toolkitbridge.exception;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Invalid API key");
    }

    public InvalidApiKeyException(String message) {
        super(message);
    }
}