package fr.ses10doigts.toolkitbridge.exception;

public class ForbiddenCommandException extends RuntimeException {
    public ForbiddenCommandException(String message) {
        super(message);
    }
}