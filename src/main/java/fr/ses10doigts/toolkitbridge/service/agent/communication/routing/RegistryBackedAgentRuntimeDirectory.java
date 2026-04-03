package fr.ses10doigts.toolkitbridge.service.agent.communication.routing;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistryBackedAgentRuntimeDirectory implements AgentRuntimeDirectory {

    private final AgentRuntimeRegistry runtimeRegistry;
    private final AgentDefinitionService definitionService;
    private final AgentAccountService agentAccountService;

    @Override
    public Optional<AgentRuntime> findByAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return Optional.empty();
        }

        return runtimeRegistry.findByAgentId(agentId)
                .or(() -> definitionService.findById(agentId)
                        .flatMap(this::createRuntimeForDefinition));
    }

    @Override
    public List<AgentRuntime> findByRole(AgentRole role) {
        if (role == null) {
            return List.of();
        }

        List<AgentRuntime> runtimes = new ArrayList<>();
        for (AgentDefinition definition : definitionService.findAll()) {
            if (definition.role() != role) {
                continue;
            }
            createRuntimeForDefinition(definition).ifPresent(runtimes::add);
        }
        return List.copyOf(runtimes);
    }

    private Optional<AgentRuntime> createRuntimeForDefinition(AgentDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) {
            return Optional.empty();
        }
        try {
            AuthenticatedAgent authenticated = agentAccountService.authenticateByAgentIdent(definition.id());
            return Optional.of(runtimeRegistry.getOrCreate(definition, authenticated));
        } catch (Exception e) {
            log.debug("Skipping runtime creation for agentId={} due to authentication/runtime init issue",
                    definition.id(),
                    e);
            return Optional.empty();
        }
    }
}
