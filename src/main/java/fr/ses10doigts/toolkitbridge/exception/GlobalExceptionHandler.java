package fr.ses10doigts.toolkitbridge.exception;

import fr.ses10doigts.toolkitbridge.model.dto.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentAlreadyExistsException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleAgentAlreadyExists(
            AgentAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Agent already exists: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AgentNotFoundException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleAgentNotFound(
            AgentNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Agent not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleInvalidApiKey(
            InvalidApiKeyException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid API key: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(ForbiddenCommandException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleForbiddenCommand(
            ForbiddenCommandException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden command: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AgentPermissionDeniedException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handlePermissionDenied(
            AgentPermissionDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Permission denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request, null);
    }

    private org.springframework.http.ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details
        );

        return org.springframework.http.ResponseEntity.status(status).body(body);
    }
}
