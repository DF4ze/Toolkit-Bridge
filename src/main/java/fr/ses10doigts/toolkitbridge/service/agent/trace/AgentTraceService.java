package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;

import java.util.Map;

public interface AgentTraceService {

    void trace(AgentTraceEventType type,
               String source,
               AgentTraceCorrelation correlation,
               Map<String, Object> attributes);
}
