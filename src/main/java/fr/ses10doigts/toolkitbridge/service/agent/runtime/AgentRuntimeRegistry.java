package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class AgentRuntimeRegistry {

    private final AgentRuntimeFactory runtimeFactory;
    private final ConcurrentMap<String, AgentRuntime> runtimesByAgentId = new ConcurrentHashMap<>();

    public AgentRuntime getOrCreate(AgentDefinition definition, AuthenticatedAgent authenticatedAgent) {
        return runtimesByAgentId.computeIfAbsent(
                definition.id(),
                ignored -> runtimeFactory.create(definition, authenticatedAgent)
        );
    }

    public Optional<AgentRuntime> findByAgentId(String agentId) {
        return Optional.ofNullable(runtimesByAgentId.get(agentId));
    }

    public List<AgentRuntime> findByRole(AgentRole role) {
        return runtimesByAgentId.values().stream()
                .filter(runtime -> runtime.role() == role)
                .toList();
    }

    public List<AgentRuntime> findAll() {
        return List.copyOf(runtimesByAgentId.values());
    }
}
