package fr.ses10doigts.toolkitbridge.service.reload;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReloadReportTest {

    @Test
    void shouldDeriveGlobalSuccessWhenAllDomainResultsSucceed() {
        ReloadReport report = ReloadReport.of(
                Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION),
                List.of(ReloadDomainResult.success(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION, "ok")),
                Instant.now(),
                Instant.now(),
                0
        );

        assertThat(report.status()).isEqualTo(ReloadReportStatus.SUCCESS);
    }

    @Test
    void shouldDerivePartialSuccessWhenMixingSuccessAndNonSuccessResults() {
        ReloadReport report = ReloadReport.of(
                Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION, ReloadDomain.LLM_PROVIDER_REGISTRY),
                List.of(
                        ReloadDomainResult.success(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION, "ok"),
                        ReloadDomainResult.unsupported(ReloadDomain.LLM_PROVIDER_REGISTRY, "unsupported")
                ),
                Instant.now(),
                Instant.now(),
                1
        );

        assertThat(report.status()).isEqualTo(ReloadReportStatus.PARTIAL_SUCCESS);
    }

    @Test
    void shouldDeriveFailedWhenNoSuccessfulDomainResultExists() {
        ReloadReport report = ReloadReport.of(
                Set.of(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION),
                List.of(ReloadDomainResult.failed(
                        ReloadDomain.MEMORY_RUNTIME_CONFIGURATION,
                        ReloadErrorType.RELOAD_EXECUTION_FAILED,
                        "failed"
                )),
                Instant.now(),
                Instant.now(),
                1
        );

        assertThat(report.status()).isEqualTo(ReloadReportStatus.FAILED);
    }

    @Test
    void shouldPreserveStableRequestedDomainsIterationOrder() {
        LinkedHashSet<ReloadDomain> requested = new LinkedHashSet<>();
        requested.add(ReloadDomain.LLM_PROVIDER_REGISTRY);
        requested.add(ReloadDomain.MEMORY_RUNTIME_CONFIGURATION);

        ReloadReport report = ReloadReport.of(
                requested,
                List.of(),
                Instant.now(),
                Instant.now(),
                0
        );

        assertThat(report.requestedDomains()).containsExactly(
                ReloadDomain.LLM_PROVIDER_REGISTRY,
                ReloadDomain.MEMORY_RUNTIME_CONFIGURATION
        );
    }
}
