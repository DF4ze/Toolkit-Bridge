package fr.ses10doigts.toolkitbridge.service.reload;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReloadDomainResultTest {

    @Test
    void shouldRequireErrorTypeForFailedStatus() {
        assertThatThrownBy(() -> new ReloadDomainResult(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadStatus.FAILED,
                null,
                "failed"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null for failed reload");
    }

    @Test
    void shouldEnforceUnsupportedDomainErrorTypeForUnsupportedStatus() {
        assertThatThrownBy(() -> new ReloadDomainResult(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadStatus.UNSUPPORTED,
                ReloadErrorType.RELOAD_EXECUTION_FAILED,
                "unsupported"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported reload must use UNSUPPORTED_DOMAIN");
    }

    @Test
    void shouldRejectErrorTypeForSuccessfulStatus() {
        assertThatThrownBy(() -> new ReloadDomainResult(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadStatus.SUCCESS,
                ReloadErrorType.INVALID_HANDLER_RESULT,
                "ok"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null for successful reload");
    }

    @Test
    void shouldCreateUnsupportedResultWithExpectedContract() {
        ReloadDomainResult result = ReloadDomainResult.unsupported(
                ReloadDomain.LLM_PROVIDER_REGISTRY,
                "No handler"
        );

        assertThat(result.status()).isEqualTo(ReloadStatus.UNSUPPORTED);
        assertThat(result.errorType()).isEqualTo(ReloadErrorType.UNSUPPORTED_DOMAIN);
        assertThat(result.message()).isEqualTo("No handler");
    }

    @Test
    void shouldSanitizeLongMessages() {
        String longMessage = "x".repeat(300);
        ReloadDomainResult result = ReloadDomainResult.failed(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadErrorType.RELOAD_EXECUTION_FAILED,
                longMessage
        );

        assertThat(result.message()).hasSize(240);
    }
}
