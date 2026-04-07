package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AgentTraceContextHolder {

    private final ThreadLocal<AgentTraceCorrelation> correlationHolder = new ThreadLocal<>();

    public void setCurrentCorrelation(AgentTraceCorrelation correlation) {
        if (correlation == null) {
            correlationHolder.remove();
            return;
        }
        correlationHolder.set(correlation);
    }

    public Optional<AgentTraceCorrelation> getCurrentCorrelation() {
        return Optional.ofNullable(correlationHolder.get());
    }

    public void clear() {
        correlationHolder.remove();
    }
}
