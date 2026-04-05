package fr.ses10doigts.toolkitbridge.service.workspace;

import fr.ses10doigts.toolkitbridge.config.agent.AgentsProperties;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinitionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceInitializer implements ApplicationRunner {

    private final WorkspaceLayout workspaceLayout;
    private final AgentsProperties agentsProperties; // adapte le type réel

    @Override
    public void run(@NonNull ApplicationArguments unused) throws IOException {
        Path botsRoot = workspaceLayout.agentsRoot();
        Path sharedRoot = workspaceLayout.sharedRoot();
        Path globalContextRoot = workspaceLayout.globalContextRoot();

        List<AgentDefinitionProperties> agents = agentsProperties.getDefinitions(); // adapte selon ton modèle
        if (agents == null || agents.isEmpty()) {
            log.info("No agent definitions found, only root workspaces created");
            return;
        }

        for (AgentDefinitionProperties agent : agents) {
            Path agentWorkspace = workspaceLayout.agentWorkspace(agent.getId());
            log.info("Workspace initialized for agent '{}' at {}", agent.getId(), agentWorkspace);
        }

        log.info("Shared workspace initialized at {}", sharedRoot);
        log.info("Global context root initialized at {}", globalContextRoot);
    }
}
