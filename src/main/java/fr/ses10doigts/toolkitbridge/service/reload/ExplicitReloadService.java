package fr.ses10doigts.toolkitbridge.service.reload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ExplicitReloadService {

    private final Map<ReloadDomain, ReloadDomainHandler> handlersByDomain;

    public ExplicitReloadService(List<ReloadDomainHandler> handlers) {
        this.handlersByDomain = indexHandlersByDomain(handlers == null ? List.of() : handlers);
    }

    public ReloadReport reload(ReloadDomain domain) {
        if (domain == null) {
            throw new IllegalArgumentException("domain must not be null");
        }
        return reload(Set.of(domain));
    }

    public ReloadReport reload(Set<ReloadDomain> domains) {
        if (domains == null || domains.isEmpty()) {
            throw new IllegalArgumentException("domains must not be null or empty");
        }

        Instant startedAt = Instant.now();
        LinkedHashSet<ReloadDomain> requestedDomains = new LinkedHashSet<>(domains);
        List<ReloadDomainResult> results = new ArrayList<>(requestedDomains.size());

        log.info("Explicit reload start domains={}", requestedDomains);
        for (ReloadDomain domain : requestedDomains) {
            results.add(reloadDomain(domain));
        }

        Instant endedAt = Instant.now();
        long durationMs = Math.max(0, Duration.between(startedAt, endedAt).toMillis());
        ReloadReport report = ReloadReport.of(requestedDomains, results, startedAt, endedAt, durationMs);
        log.info("Explicit reload end status={} durationMs={} domains={}",
                report.status(),
                report.durationMs(),
                requestedDomains);
        return report;
    }

    private ReloadDomainResult reloadDomain(ReloadDomain domain) {
        ReloadDomainHandler handler = handlersByDomain.get(domain);
        if (handler == null) {
            String message = "No reload handler registered for domain";
            log.warn("Explicit reload unsupported domain={} reason={}", domain, message);
            return ReloadDomainResult.unsupported(domain, message);
        }

        log.info("Explicit reload domain start domain={}", domain);
        try {
            ReloadDomainResult result = handler.reload();
            ReloadDomainResult normalized = normalizeHandlerResult(domain, result);
            if (normalized.status() == ReloadStatus.SUCCESS) {
                log.info("Explicit reload domain success domain={}", domain);
            } else {
                log.warn("Explicit reload domain non-success domain={} status={} errorType={} message={}",
                        domain,
                        normalized.status(),
                        normalized.errorType(),
                        normalized.message());
            }
            return normalized;
        } catch (Exception e) {
            String message = "Reload execution failed";
            log.error("Explicit reload domain failure domain={} reason={}", domain, message, e);
            return ReloadDomainResult.failed(domain, ReloadErrorType.RELOAD_EXECUTION_FAILED, message);
        }
    }

    private ReloadDomainResult normalizeHandlerResult(ReloadDomain requestedDomain, ReloadDomainResult result) {
        if (result == null) {
            return ReloadDomainResult.failed(
                    requestedDomain,
                    ReloadErrorType.INVALID_HANDLER_RESULT,
                    "Reload handler returned no result"
            );
        }
        if (result.domain() != requestedDomain) {
            return ReloadDomainResult.failed(
                    requestedDomain,
                    ReloadErrorType.INVALID_HANDLER_RESULT,
                    "Reload handler returned mismatched domain result"
            );
        }
        return result;
    }

    private Map<ReloadDomain, ReloadDomainHandler> indexHandlersByDomain(List<ReloadDomainHandler> handlers) {
        EnumMap<ReloadDomain, ReloadDomainHandler> map = new EnumMap<>(ReloadDomain.class);
        for (ReloadDomainHandler handler : handlers) {
            if (handler == null) {
                throw new IllegalStateException("ReloadDomainHandler list contains null handler");
            }
            ReloadDomain domain = handler.domain();
            if (domain == null) {
                throw new IllegalStateException("ReloadDomainHandler returned null domain");
            }
            ReloadDomainHandler previous = map.putIfAbsent(domain, handler);
            if (previous != null) {
                throw new IllegalStateException("Multiple reload handlers registered for domain " + domain);
            }
        }
        return Map.copyOf(map);
    }
}
