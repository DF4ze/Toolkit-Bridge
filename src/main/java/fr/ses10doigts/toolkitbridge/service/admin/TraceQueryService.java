package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceQueryService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class TraceQueryService {

    private final AgentTraceQueryService agentTraceQueryService;
    private final AdminTechnicalProperties technicalProperties;

    public TraceQueryService(AgentTraceQueryService agentTraceQueryService, AdminTechnicalProperties technicalProperties) {
        this.agentTraceQueryService = agentTraceQueryService;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.TraceItem> listRecentTraces(Integer limit, String agentId) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<AgentTraceEvent> source = isBlank(agentId)
                ? agentTraceQueryService.recentEvents()
                : agentTraceQueryService.recentEventsForAgent(agentId);

        return source.stream()
                .sorted(Comparator.comparing(AgentTraceEvent::occurredAt).reversed())
                .limit(effectiveLimit)
                .map(this::toTraceItem)
                .toList();
    }

    private TechnicalAdminView.TraceItem toTraceItem(AgentTraceEvent trace) {
        return new TechnicalAdminView.TraceItem(
                trace.occurredAt(),
                trace.type(),
                trace.source(),
                trace.correlation() == null ? null : trace.correlation().runId(),
                trace.correlation() == null ? null : trace.correlation().agentId(),
                trace.correlation() == null ? null : trace.correlation().messageId(),
                trace.correlation() == null ? null : trace.correlation().taskId(),
                trace.attributes()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

