package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.context.global.port.SharedGlobalContextProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExplicitReloadServiceTest {

    @Test
    void shouldReturnSuccessWhenAllRequestedDomainsSucceed() {
        ExplicitReloadService service = new ExplicitReloadService(List.of(
                successHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION),
                successHandler(ReloadDomain.LLM_PROVIDER_REGISTRY)
        ));

        ReloadReport report = service.reload(Set.of(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadDomain.LLM_PROVIDER_REGISTRY
        ));

        assertThat(report.status()).isEqualTo(ReloadReportStatus.SUCCESS);
        assertThat(report.domainResults()).hasSize(2);
        assertThat(report.domainResults()).allMatch(result -> result.status() == ReloadStatus.SUCCESS);
    }

    @Test
    void shouldReturnPartialSuccessWhenAtLeastOneDomainIsUnsupportedAndAnotherSucceeds() {
        ExplicitReloadService service = new ExplicitReloadService(List.of(
                successHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION)
        ));

        ReloadReport report = service.reload(Set.of(
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                ReloadDomain.LLM_PROVIDER_REGISTRY
        ));

        assertThat(report.status()).isEqualTo(ReloadReportStatus.PARTIAL_SUCCESS);
        assertThat(findResult(report, ReloadDomain.MEMORY_RUNTIME_CONFIGURATION).status()).isEqualTo(ReloadStatus.SUCCESS);
        assertThat(findResult(report, ReloadDomain.LLM_PROVIDER_REGISTRY).status()).isEqualTo(ReloadStatus.UNSUPPORTED);
        assertThat(findResult(report, ReloadDomain.LLM_PROVIDER_REGISTRY).errorType())
                .isEqualTo(ReloadErrorType.UNSUPPORTED_DOMAIN);
    }

    @Test
    void shouldReturnFailedWhenAllRequestedDomainsAreUnsupported() {
        ExplicitReloadService service = new ExplicitReloadService(List.of());

        ReloadReport report = service.reload(Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION));

        assertThat(report.status()).isEqualTo(ReloadReportStatus.FAILED);
        assertThat(report.domainResults()).singleElement().satisfies(result -> {
            assertThat(result.status()).isEqualTo(ReloadStatus.UNSUPPORTED);
            assertThat(result.errorType()).isEqualTo(ReloadErrorType.UNSUPPORTED_DOMAIN);
        });
    }

    @Test
    void shouldCaptureHandlerExceptionAsFailedDomainResultWithoutStackTraceInMessage() {
        ExplicitReloadService service = new ExplicitReloadService(List.of(
                failingHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION)
        ));

        ReloadReport report = service.reload(Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION));

        ReloadDomainResult result = report.domainResults().getFirst();
        assertThat(report.status()).isEqualTo(ReloadReportStatus.FAILED);
        assertThat(result.status()).isEqualTo(ReloadStatus.FAILED);
        assertThat(result.errorType()).isEqualTo(ReloadErrorType.RELOAD_EXECUTION_FAILED);
        assertThat(result.message()).isEqualTo("Reload execution failed");
        assertThat(result.message()).doesNotContain("\n");
    }

    @Test
    void shouldFailFastWhenMultipleHandlersAreRegisteredForTheSameDomain() {
        assertThatThrownBy(() -> new ExplicitReloadService(List.of(
                successHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION),
                successHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple reload handlers registered");
    }

    @Test
    void shouldFailFastWhenNullHandlerIsRegistered() {
        assertThatThrownBy(() -> new ExplicitReloadService(java.util.Arrays.asList(
                successHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION),
                null
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains null handler");
    }

    @Test
    void shouldRejectEmptyDomainRequest() {
        ExplicitReloadService service = new ExplicitReloadService(List.of());

        assertThatThrownBy(() -> service.reload(Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("domains must not be null or empty");
    }

    @Test
    void shouldRejectNullSingleDomainRequest() {
        ExplicitReloadService service = new ExplicitReloadService(List.of());

        assertThatThrownBy(() -> service.reload((ReloadDomain) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("domain must not be null");
    }

    @Test
    void shouldNormalizeMismatchedHandlerResultDomainToFailed() {
        ExplicitReloadService service = new ExplicitReloadService(List.of(
                mismatchedResultHandler(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION)
        ));

        ReloadReport report = service.reload(Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION));

        ReloadDomainResult result = report.domainResults().getFirst();
        assertThat(report.status()).isEqualTo(ReloadReportStatus.FAILED);
        assertThat(result.domain()).isEqualTo(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);
        assertThat(result.status()).isEqualTo(ReloadStatus.FAILED);
        assertThat(result.errorType()).isEqualTo(ReloadErrorType.INVALID_HANDLER_RESULT);
        assertThat(result.message()).isEqualTo("Reload handler returned mismatched domain result");
    }

    @Test
    void shouldUseMemoryRuntimeConfigurationHandlerThroughOrchestrator() {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        SharedGlobalContextProvider globalContextProvider = mock(SharedGlobalContextProvider.class);
        when(resolver.reloadFromDatabase()).thenReturn(true);
        MemoryRuntimeConfigurationReloadHandler handler = new MemoryRuntimeConfigurationReloadHandler(
                resolver,
                globalContextProvider
        );

        ExplicitReloadService service = new ExplicitReloadService(List.of(handler));

        ReloadReport report = service.reload(Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION));

        assertThat(report.status()).isEqualTo(ReloadReportStatus.SUCCESS);
        assertThat(report.domainResults()).singleElement().satisfies(result -> {
            assertThat(result.domain()).isEqualTo(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);
            assertThat(result.status()).isEqualTo(ReloadStatus.SUCCESS);
        });
        verify(globalContextProvider).invalidateCache();
    }

    private ReloadDomainResult findResult(ReloadReport report, ReloadDomain domain) {
        return report.domainResults().stream()
                .filter(result -> result.domain() == domain)
                .findFirst()
                .orElseThrow();
    }

    private ReloadDomainHandler successHandler(ReloadDomain domain) {
        return new ReloadDomainHandler() {
            @Override
            public ReloadDomain domain() {
                return domain;
            }

            @Override
            public ReloadDomainResult reload() {
                return ReloadDomainResult.success(domain, "Reload completed");
            }
        };
    }

    private ReloadDomainHandler failingHandler(ReloadDomain domain) {
        return new ReloadDomainHandler() {
            @Override
            public ReloadDomain domain() {
                return domain;
            }

            @Override
            public ReloadDomainResult reload() {
                throw new IllegalStateException("boom");
            }
        };
    }

    private ReloadDomainHandler mismatchedResultHandler(ReloadDomain domain) {
        return new ReloadDomainHandler() {
            @Override
            public ReloadDomain domain() {
                return domain;
            }

            @Override
            public ReloadDomainResult reload() {
                return ReloadDomainResult.success(ReloadDomain.LLM_PROVIDER_REGISTRY, "Wrong domain");
            }
        };
    }
}
