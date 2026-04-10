package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.CriticalTraceSanitizationProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class CriticalAgentTraceMapper {

    private static final Set<AgentTraceEventType> CRITICAL_TYPES = EnumSet.of(
            AgentTraceEventType.ERROR,
            AgentTraceEventType.TASK_STARTED,
            AgentTraceEventType.RESPONSE,
            AgentTraceEventType.DELEGATION,
            AgentTraceEventType.TOOL_CALL
    );
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final CriticalTraceSanitizationProperties sanitizationProperties;

    public CriticalAgentTraceMapper(ObjectMapper objectMapper,
                                    CriticalTraceSanitizationProperties sanitizationProperties) {
        this.objectMapper = objectMapper;
        this.sanitizationProperties = sanitizationProperties;
    }

    public boolean isCriticalType(AgentTraceEventType type) {
        return type != null && CRITICAL_TYPES.contains(type);
    }

    public CriticalAgentTraceEntity toEntity(AgentTraceEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        CriticalAgentTraceEntity entity = new CriticalAgentTraceEntity();
        entity.setOccurredAt(event.occurredAt() == null ? Instant.now() : event.occurredAt());
        entity.setEventType(event.type());
        entity.setSource(normalizeText(event.source(), 120, "unknown"));

        AgentTraceCorrelation correlation = event.correlation();
        entity.setRunId(correlation == null ? null : normalizeText(correlation.runId(), 200, null));
        // agentId is a technical identifier, normalized to lower-case.
        // It is not intended for UI display.
        entity.setAgentId(correlation == null ? null : normalizeAgentId(correlation.agentId()));
        entity.setMessageId(correlation == null ? null : normalizeText(correlation.messageId(), 200, null));
        entity.setTaskId(correlation == null ? null : normalizeText(correlation.taskId(), 200, null));

        Map<String, Object> sanitizedAttributes = sanitizeAttributes(event.type(), event.attributes());
        entity.setAttributesJson(toAttributesJson(sanitizedAttributes));
        entity.setIngestedAt(Instant.now());
        return entity;
    }

    private Map<String, Object> sanitizeAttributes(AgentTraceEventType eventType, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (index >= sanitizationProperties.getMaxMapItems()) {
                break;
            }
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }

            String key = entry.getKey().trim();
            Object value = sanitizeValue(eventType, key, entry.getValue(), 0);
            if (value == null) {
                continue;
            }

            sanitized.put(key, value);
            index++;
        }
        return Collections.unmodifiableMap(sanitized);
    }

    private Object sanitizeValue(AgentTraceEventType eventType, String key, Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (depth > sanitizationProperties.getMaxDepth()) {
            return truncate(value.toString(), sanitizationProperties.getMaxGenericText());
        }
        if (value instanceof String text) {
            return sanitizeTextByKey(eventType, key, text);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> mapValue) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            int index = 0;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (index >= sanitizationProperties.getMaxMapItems()) {
                    break;
                }
                if (!(entry.getKey() instanceof String nestedKey) || nestedKey.isBlank() || entry.getValue() == null) {
                    continue;
                }
                Object nestedValue = sanitizeValue(eventType, nestedKey, entry.getValue(), depth + 1);
                if (nestedValue != null) {
                    nested.put(nestedKey, nestedValue);
                    index++;
                }
            }
            return nested;
        }
        if (value instanceof List<?> listValue) {
            return sanitizeCollection(eventType, key, listValue, depth);
        }
        if (value instanceof Iterable<?> iterableValue) {
            List<Object> list = new ArrayList<>();
            for (Object item : iterableValue) {
                list.add(item);
            }
            return sanitizeCollection(eventType, key, list, depth);
        }
        return truncate(value.toString(), sanitizationProperties.getMaxGenericText());
    }

    private List<Object> sanitizeCollection(AgentTraceEventType eventType, String key, List<?> values, int depth) {
        if (values.isEmpty()) {
            return List.of();
        }
        List<Object> sanitized = new ArrayList<>();
        int max = Math.min(values.size(), sanitizationProperties.getMaxCollectionItems());
        for (int i = 0; i < max; i++) {
            Object item = sanitizeValue(eventType, key, values.get(i), depth + 1);
            if (item != null) {
                sanitized.add(item);
            }
        }
        return List.copyOf(sanitized);
    }

    private String sanitizeTextByKey(AgentTraceEventType eventType, String key, String text) {
        if (eventType == AgentTraceEventType.TOOL_CALL && "message".equals(key)) {
            return truncate(text, sanitizationProperties.getMaxToolMessageText());
        }
        if (eventType == AgentTraceEventType.ERROR && "reason".equals(key)) {
            return truncate(text, sanitizationProperties.getMaxErrorReasonText());
        }
        return truncate(text, sanitizationProperties.getMaxGenericText());
    }

    private String toAttributesJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (Exception e) {
            log.warn("Failed to serialize critical trace attributes", e);
            return "{}";
        }
    }

    public Map<String, Object> parseAttributesJson(String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(attributesJson, MAP_TYPE);
            if (parsed == null || parsed.isEmpty()) {
                return Map.of();
            }
            return Map.copyOf(parsed);
        } catch (Exception e) {
            log.warn("Failed to parse critical trace attributes JSON", e);
            return Map.of();
        }
    }

    private String normalizeText(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return truncate(value.trim(), maxLength);
    }

    private String normalizeAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        return truncate(agentId.trim().toLowerCase(Locale.ROOT), 120);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength < 1 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
