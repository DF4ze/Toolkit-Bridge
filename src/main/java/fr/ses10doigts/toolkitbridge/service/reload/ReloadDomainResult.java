package fr.ses10doigts.toolkitbridge.service.reload;

public record ReloadDomainResult(
        ReloadDomain domain,
        ReloadStatus status,
        ReloadErrorType errorType,
        String message
) {

    public ReloadDomainResult {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status == ReloadStatus.SUCCESS && errorType != null) {
            throw new IllegalArgumentException("errorType must be null for successful reload");
        }
        if (status == ReloadStatus.FAILED && errorType == null) {
            throw new IllegalArgumentException("errorType must not be null for failed reload");
        }
        if (status == ReloadStatus.UNSUPPORTED && errorType != ReloadErrorType.UNSUPPORTED_DOMAIN) {
            throw new IllegalArgumentException("unsupported reload must use UNSUPPORTED_DOMAIN errorType");
        }
    }

    public static ReloadDomainResult success(ReloadDomain domain, String message) {
        return new ReloadDomainResult(domain, ReloadStatus.SUCCESS, null, sanitizeMessage(message));
    }

    public static ReloadDomainResult failed(ReloadDomain domain, ReloadErrorType errorType, String message) {
        return new ReloadDomainResult(domain, ReloadStatus.FAILED, requireErrorType(errorType), sanitizeMessage(message));
    }

    public static ReloadDomainResult unsupported(ReloadDomain domain, String message) {
        return new ReloadDomainResult(
                domain,
                ReloadStatus.UNSUPPORTED,
                ReloadErrorType.UNSUPPORTED_DOMAIN,
                sanitizeMessage(message)
        );
    }

    private static ReloadErrorType requireErrorType(ReloadErrorType errorType) {
        if (errorType == null) {
            throw new IllegalArgumentException("errorType must not be null for failed reload");
        }
        return errorType;
    }

    private static String sanitizeMessage(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= 240) {
            return trimmed;
        }
        return trimmed.substring(0, 240);
    }
}
