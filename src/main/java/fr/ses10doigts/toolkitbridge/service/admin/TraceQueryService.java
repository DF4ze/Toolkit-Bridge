package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceEntity;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceMapper;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class TraceQueryService {

    private final CriticalAgentTraceRepository criticalTraceRepository;
    private final CriticalAgentTraceMapper criticalTraceMapper;
    private final AdminTechnicalProperties technicalProperties;

    public TraceQueryService(CriticalAgentTraceRepository criticalTraceRepository,
                             CriticalAgentTraceMapper criticalTraceMapper,
                             AdminTechnicalProperties technicalProperties) {
        this.criticalTraceRepository = criticalTraceRepository;
        this.criticalTraceMapper = criticalTraceMapper;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.TraceItem> listRecentTraces(Integer limit, String agentId) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit);
        String normalizedAgentId = normalize(agentId);

        List<CriticalAgentTraceEntity> source = normalizedAgentId == null
                ? criticalTraceRepository.findByOrderByOccurredAtDescIdDesc(pageRequest)
                : criticalTraceRepository.findByAgentIdOrderByOccurredAtDescIdDesc(normalizedAgentId, pageRequest);

        return source.stream()
                .map(this::toTraceItem)
                .toList();
    }

    private TechnicalAdminView.TraceItem toTraceItem(CriticalAgentTraceEntity entity) {
        return new TechnicalAdminView.TraceItem(
                entity.getOccurredAt(),
                entity.getEventType(),
                entity.getSource(),
                entity.getRunId(),
                entity.getAgentId(),
                entity.getMessageId(),
                entity.getTaskId(),
                criticalTraceMapper.parseAttributesJson(entity.getAttributesJson())
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // agentId is a technical identifier, normalized to lower-case.
        // It is not intended for UI display.
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
