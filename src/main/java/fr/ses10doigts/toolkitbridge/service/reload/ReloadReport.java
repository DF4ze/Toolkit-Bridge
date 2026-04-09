package fr.ses10doigts.toolkitbridge.service.reload;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public record ReloadReport(
        Set<ReloadDomain> requestedDomains,
        List<ReloadDomainResult> domainResults,
        ReloadReportStatus status,
        Instant startedAt,
        Instant endedAt,
        long durationMs
) {

    public ReloadReport {
        requestedDomains = requestedDomains == null ? Set.of() : stableOrderedSet(requestedDomains);
        domainResults = domainResults == null ? List.of() : List.copyOf(domainResults);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (startedAt == null) {
            throw new IllegalArgumentException("startedAt must not be null");
        }
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt must not be null");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be >= 0");
        }
    }

    public static ReloadReport of(Set<ReloadDomain> requestedDomains,
                                  List<ReloadDomainResult> domainResults,
                                  Instant startedAt,
                                  Instant endedAt,
                                  long durationMs) {
        return new ReloadReport(
                requestedDomains,
                domainResults,
                deriveGlobalStatus(domainResults),
                startedAt,
                endedAt,
                durationMs
        );
    }

    static ReloadReportStatus deriveGlobalStatus(List<ReloadDomainResult> domainResults) {
        if (domainResults == null || domainResults.isEmpty()) {
            return ReloadReportStatus.FAILED;
        }

        boolean hasSuccess = domainResults.stream().anyMatch(result -> result.status() == ReloadStatus.SUCCESS);
        boolean hasNonSuccess = domainResults.stream().anyMatch(result -> result.status() != ReloadStatus.SUCCESS);

        if (hasSuccess && !hasNonSuccess) {
            return ReloadReportStatus.SUCCESS;
        }
        if (hasSuccess) {
            return ReloadReportStatus.PARTIAL_SUCCESS;
        }
        return ReloadReportStatus.FAILED;
    }

    private static Set<ReloadDomain> stableOrderedSet(Set<ReloadDomain> domains) {
        LinkedHashMap<ReloadDomain, Boolean> ordered = new LinkedHashMap<>();
        for (ReloadDomain domain : new LinkedHashSet<>(domains)) {
            ordered.put(domain, Boolean.TRUE);
        }
        return java.util.Collections.unmodifiableSet(ordered.keySet());
    }
}
