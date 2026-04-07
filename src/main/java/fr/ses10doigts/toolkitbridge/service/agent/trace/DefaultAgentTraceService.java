package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.AgentTraceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.agent.trace.sink.AgentTraceSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DefaultAgentTraceService implements AgentTraceService {

    private final List<AgentTraceSink> sinks;
    private final AgentTraceProperties properties;

    public DefaultAgentTraceService(List<AgentTraceSink> sinks, AgentTraceProperties properties) {
        this.sinks = sinks == null ? List.of() : List.copyOf(sinks);
        this.properties = properties;
    }

    @Override
    public void trace(AgentTraceEventType type,
                      String source,
                      AgentTraceCorrelation correlation,
                      Map<String, Object> attributes) {
        if (!properties.isEnabled() || type == null) {
            return;
        }

        AgentTraceEvent event = new AgentTraceEvent(
                Instant.now(),
                type,
                normalizeSource(source),
                correlation,
                sanitizeAttributes(attributes)
        );

        for (AgentTraceSink sink : sinks) {
            try {
                sink.publish(event);
            } catch (RuntimeException e) {
                log.warn("Agent trace sink failure type={} source={} sink={}",
                        type,
                        source,
                        sink.getClass().getSimpleName(),
                        e);
            }
        }
    }

    private Map<String, Object> sanitizeAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            sanitized.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(sanitized);
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source.trim();
    }
}
