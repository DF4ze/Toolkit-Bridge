package fr.ses10doigts.toolkitbridge.service.agent.trace.model;

import java.time.Instant;
import java.util.Map;

public record AgentTraceEvent(
        Instant occurredAt,
        AgentTraceEventType type,
        String source,
        AgentTraceCorrelation correlation,
        Map<String, Object> attributes
) {
}
