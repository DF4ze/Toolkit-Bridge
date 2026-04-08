package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;

import java.util.List;

public interface AgentTraceQueryService {

    List<AgentTraceEvent> recentEvents();

    List<AgentTraceEvent> recentEventsForAgent(String agentId);
}
