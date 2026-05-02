package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class WorkflowTelegramErrorSanitizer {

    static final int DEFAULT_MAX_LEN = 380;

    private static final Pattern WINDOWS_ABS_PATH = Pattern.compile("(?i)\\b[A-Z]:\\\\[^\\s\\r\\n]+");
    private static final Pattern UNIX_ABS_PATH = Pattern.compile("(?m)(^|\\s)/[^\\s\\r\\n]+");

    private static final Pattern INLINE_TOKEN = Pattern.compile("(?i)\\b(token|api[-_ ]?key|authorization|bearer)\\b\\s*[:=]\\s*[^\\s\\r\\n]+");
    private static final Pattern LONG_HEX = Pattern.compile("(?i)\\b[0-9a-f]{32,}\\b");

    private static final Pattern STACKTRACE_LINE = Pattern.compile("(?m)^\\s*at\\s+.+$");

    private static final Pattern RATE_LIMIT = Pattern.compile("(?i)\\b(quota|rate\\s*limit|rate-limit|too\\s+many\\s+requests|usage\\s+limit|limit\\s+reached|weekly\\s+limit|5h\\s+limit|\\b429\\b)\\b");

    public SanitizedError sanitizeForTelegram(String raw) {
        return sanitizeForTelegram(raw, DEFAULT_MAX_LEN);
    }

    public SanitizedError sanitizeForTelegram(String raw, int maxLen) {
        String safeRaw = raw == null ? "" : raw;
        String normalized = safeRaw.replace("\r\n", "\n");

        boolean rateLimit = isRateLimit(normalized);
        if (rateLimit) {
            return SanitizedError.rateLimit(
                    "Provider quota or rate limit reached.",
                    "Wait and retry later, or switch provider/model."
            );
        }

        String stripped = stripStacktraceMarkers(normalized);
        stripped = redactSecrets(stripped);
        stripped = redactPaths(stripped);
        stripped = collapseWhitespace(stripped).trim();

        if (stripped.isBlank()) {
            stripped = "Unknown error";
        }

        return SanitizedError.generic(truncate(stripped, maxLen));
    }

    public boolean isRateLimit(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return RATE_LIMIT.matcher(raw).find();
    }

    private String stripStacktraceMarkers(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        int firstTabAt = value.indexOf("\tat");
        if (firstTabAt >= 0) {
            value = value.substring(0, firstTabAt);
        }

        int firstNewlineAt = value.indexOf("\nat ");
        if (firstNewlineAt >= 0) {
            value = value.substring(0, firstNewlineAt);
        }

        value = STACKTRACE_LINE.matcher(value).replaceAll("");

        int exceptionIndex = value.toLowerCase(Locale.ROOT).indexOf("exception");
        if (exceptionIndex >= 0 && exceptionIndex <= 10) {
            value = value.substring(0, exceptionIndex);
        }

        return value;
    }

    private String redactPaths(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        value = WINDOWS_ABS_PATH.matcher(value).replaceAll("[path]");
        value = UNIX_ABS_PATH.matcher(value).replaceAll("$1[path]");
        return value;
    }

    private String redactSecrets(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        value = INLINE_TOKEN.matcher(value).replaceAll("$1=[redacted]");
        value = LONG_HEX.matcher(value).replaceAll("[redacted]");
        return value;
    }

    private String collapseWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\t\\x0B\\f]+", " ").replaceAll(" +", " ").replaceAll("\\n{3,}", "\n\n");
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        int limit = Math.max(50, maxLen);
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit - 3).trim() + "...";
    }

    public record SanitizedError(boolean rateLimit, String reason, String userHint) {
        static SanitizedError rateLimit(String reason, String userHint) {
            return new SanitizedError(true, reason, userHint);
        }

        static SanitizedError generic(String reason) {
            return new SanitizedError(false, reason, null);
        }
    }
}
