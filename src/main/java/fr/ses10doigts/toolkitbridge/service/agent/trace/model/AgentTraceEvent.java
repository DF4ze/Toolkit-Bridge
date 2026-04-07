package fr.ses10doigts.toolkitbridge.service.agent.trace.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;

import java.time.Instant;
import java.util.Map;

public record AgentTraceEvent(
        Instant occurredAt,
        AgentTraceEventType type,
        String source,
        AgentTraceCorrelation correlation,
        Map<String, Object> attributes
) implements DurableObject {

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.TRACE;
    }
}
