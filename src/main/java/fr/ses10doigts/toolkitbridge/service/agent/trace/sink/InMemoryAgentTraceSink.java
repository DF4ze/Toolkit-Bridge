package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.AgentTraceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class InMemoryAgentTraceSink implements AgentTraceSink {

    private final AgentTraceProperties properties;
    private final Deque<AgentTraceEvent> recentEvents = new ArrayDeque<>();

    public InMemoryAgentTraceSink(AgentTraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void publish(AgentTraceEvent event) {
        if (!properties.getMemory().isEnabled() || event == null) {
            return;
        }

        recentEvents.addLast(event);
        int maxEvents = Math.max(properties.getMemory().getMaxEvents(), 1);
        while (recentEvents.size() > maxEvents) {
            recentEvents.removeFirst();
        }
    }

    public synchronized List<AgentTraceEvent> recentEvents() {
        return List.copyOf(recentEvents);
    }

    public synchronized List<AgentTraceEvent> recentEventsForAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return List.of();
        }

        List<AgentTraceEvent> filtered = new ArrayList<>();
        for (AgentTraceEvent event : recentEvents) {
            if (event.correlation() != null && agentId.equals(event.correlation().agentId())) {
                filtered.add(event);
            }
        }
        return List.copyOf(filtered);
    }
}
