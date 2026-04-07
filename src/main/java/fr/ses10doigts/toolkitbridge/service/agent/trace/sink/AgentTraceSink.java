package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;

public interface AgentTraceSink {

    void publish(AgentTraceEvent event);
}
