package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentAccountInitializer implements ApplicationRunner {

    private final AdministrableConfigurationGateway configurationGateway;
    private final AgentAccountService agentAccountService;
    private final AgentAccountRepository agentAccountRepository;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<AgentDefinitionProperties> definitions = configurationGateway.loadAgentDefinitions();
        if (definitions == null || definitions.isEmpty()) {
            log.info("No agent definitions configured, skipping account initialization");
            return;
        }

        Set<String> declaredAgents = definitions.stream()
                .map(AgentDefinitionProperties::getId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        if (declaredAgents.isEmpty()) {
            log.info("Agent definitions are empty, skipping account initialization");
            return;
        }

        for (String agentIdent : declaredAgents) {
            if (!agentAccountRepository.existsByAgentIdent(agentIdent)) {
                AgentProvisioningResult result = agentAccountService.createBot(agentIdent);
                log.warn("Created missing agent account agentIdent='{}' apiKey='{}'. Store this key securely.",
                        result.agentIdent(),
                        result.apiKey());
            }
        }

        long removed = agentAccountRepository.deleteByAgentIdentNotIn(declaredAgents);
        if (removed > 0) {
            log.warn("Removed {} agent account(s) not declared in configuration", removed);
        }
    }
}
