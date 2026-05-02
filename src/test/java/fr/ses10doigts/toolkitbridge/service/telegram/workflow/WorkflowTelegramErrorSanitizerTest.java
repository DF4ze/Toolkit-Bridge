package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTelegramErrorSanitizerTest {

    @Test
    void removesStacktraceMarkers() {
        WorkflowTelegramErrorSanitizer sanitizer = new WorkflowTelegramErrorSanitizer();
        String raw = "Boom happened\n\tat com.acme.Foo.bar(Foo.java:12)\n\tat com.acme.Baz.qux(Baz.java:34)";

        WorkflowTelegramErrorSanitizer.SanitizedError sanitized = sanitizer.sanitizeForTelegram(raw, 380);

        assertThat(sanitized.reason()).contains("Boom happened");
        assertThat(sanitized.reason()).doesNotContain("\tat");
        assertThat(sanitized.reason()).doesNotContain("Foo.java");
    }

    @Test
    void redactsWindowsAbsolutePaths() {
        WorkflowTelegramErrorSanitizer sanitizer = new WorkflowTelegramErrorSanitizer();
        String raw = "Failed to read C:\\Users\\Bob\\secret.txt due to permission";

        WorkflowTelegramErrorSanitizer.SanitizedError sanitized = sanitizer.sanitizeForTelegram(raw, 380);

        assertThat(sanitized.reason()).doesNotContain(":\\");
        assertThat(sanitized.reason()).contains("[path]");
    }

    @Test
    void redactsUnixAbsolutePaths() {
        WorkflowTelegramErrorSanitizer sanitizer = new WorkflowTelegramErrorSanitizer();
        String raw = "Failed to read /home/bob/secret.txt due to permission";

        WorkflowTelegramErrorSanitizer.SanitizedError sanitized = sanitizer.sanitizeForTelegram(raw, 380);

        assertThat(sanitized.reason()).doesNotContain("/home/bob/secret.txt");
        assertThat(sanitized.reason()).contains("[path]");
    }

    @Test
    void truncatesVeryLongMessages() {
        WorkflowTelegramErrorSanitizer sanitizer = new WorkflowTelegramErrorSanitizer();
        String raw = "x".repeat(2000);

        WorkflowTelegramErrorSanitizer.SanitizedError sanitized = sanitizer.sanitizeForTelegram(raw, 380);

        assertThat(sanitized.reason().length()).isLessThanOrEqualTo(380);
    }

    @Test
    void detectsRateLimitPatterns() {
        WorkflowTelegramErrorSanitizer sanitizer = new WorkflowTelegramErrorSanitizer();

        assertThat(sanitizer.sanitizeForTelegram("429 Too Many Requests", 380).rateLimit()).isTrue();
        assertThat(sanitizer.sanitizeForTelegram("quota exceeded", 380).rateLimit()).isTrue();
        assertThat(sanitizer.sanitizeForTelegram("rate-limit reached", 380).rateLimit()).isTrue();
    }
}

