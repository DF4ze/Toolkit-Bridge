package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceMapper;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.springframework.stereotype.Component;

@Component
public class CriticalTraceJpaSink implements AgentTraceSink {

    private final CriticalAgentTraceRepository repository;
    private final CriticalAgentTraceMapper mapper;

    public CriticalTraceJpaSink(CriticalAgentTraceRepository repository,
                                CriticalAgentTraceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void publish(AgentTraceEvent event) {
        if (event == null || !mapper.isCriticalType(event.type())) {
            return;
        }
        repository.save(mapper.toEntity(event));
    }
}
