package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentAvailability;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FirstAvailableAgentRoleSelectionStrategy implements AgentRoleSelectionStrategy {

    @Override
    public Optional<AgentRuntime> select(List<AgentRuntime> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(runtime -> runtime.state().snapshot().availability() == AgentAvailability.AVAILABLE)
                .filter(runtime -> !runtime.state().isBusy())
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(runtime -> runtime.state().snapshot().availability() == AgentAvailability.AVAILABLE)
                        .findFirst())
                .or(() -> Optional.of(candidates.getFirst()));
    }
}

