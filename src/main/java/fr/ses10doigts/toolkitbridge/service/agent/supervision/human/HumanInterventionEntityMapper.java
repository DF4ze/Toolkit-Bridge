package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class HumanInterventionEntityMapper {

    private static final int MAX_METADATA_JSON_LENGTH = 4096;
    private static final Set<String> SENSITIVE_KEY_FRAGMENTS = Set.of("token", "secret", "password");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public HumanInterventionEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HumanInterventionEntity toEntityForOpen(String requestId, Instant createdAt, HumanInterventionDraft draft) {
        HumanInterventionEntity entity = new HumanInterventionEntity();
        entity.setRequestId(requestId);
        entity.setCreatedAt(createdAt);
        entity.setTraceId(draft.traceId());
        entity.setAgentId(draft.agentId());
        entity.setSensitiveAction(draft.sensitiveAction());
        entity.setKind(draft.kind());
        entity.setStatus(HumanInterventionStatus.PENDING);
        entity.setSummary(draft.summary());
        entity.setDetail(draft.detail());
        entity.setRequestMetadataJson(toMetadataJson(draft.metadata(), requestId, "request"));
        return entity;
    }

    public HumanInterventionRequest toDomainRequest(HumanInterventionEntity entity) {
        return new HumanInterventionRequest(
                entity.getRequestId(),
                entity.getCreatedAt(),
                entity.getTraceId(),
                entity.getAgentId(),
                entity.getSensitiveAction(),
                entity.getKind(),
                entity.getStatus(),
                entity.getSummary(),
                entity.getDetail(),
                parseMetadataJson(entity.getRequestMetadataJson())
        );
    }

    public String toDecisionMetadataJson(HumanInterventionDecision decision) {
        if (decision == null) {
            return null;
        }
        return toMetadataJson(decision.metadata(), decision.requestId(), "decision");
    }

    private String toMetadataJson(Map<String, Object> metadata, String requestId, String metadataKind) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        LinkedHashMap<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            if (isSensitiveKey(entry.getKey())) {
                continue;
            }
            filtered.put(entry.getKey(), entry.getValue());
        }

        if (filtered.isEmpty()) {
            return null;
        }

        try {
            String serialized = objectMapper.writeValueAsString(filtered);
            if (serialized.length() > MAX_METADATA_JSON_LENGTH) {
                log.warn("Human intervention {} metadata exceeded max size={} requestId={} -> storing empty object",
                        metadataKind,
                        MAX_METADATA_JSON_LENGTH,
                        requestId);
                return "{}";
            }
            return serialized;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize human intervention metadata", e);
        }
    }

    private Map<String, Object> parseMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(metadataJson, MAP_TYPE);
            return parsed == null ? Map.of() : Map.copyOf(parsed);
        } catch (Exception e) {
            log.warn("Unable to deserialize human intervention metadata JSON -> fallback empty map", e);
            return Map.of();
        }
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (String fragment : SENSITIVE_KEY_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
