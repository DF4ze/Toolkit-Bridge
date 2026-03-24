package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.exception.InvalidApiKeyException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import org.springframework.stereotype.Service;

@Service
public class CurrentAgentService {

    private final AgentContextHolder agentContextHolder;

    public CurrentAgentService(AgentContextHolder agentContextHolder) {
        this.agentContextHolder = agentContextHolder;
    }

    public AuthenticatedAgent getCurrentBot() {
        AuthenticatedAgent bot = agentContextHolder.getCurrentBot();
        if (bot == null) {
            throw new InvalidApiKeyException("Authentication is required");
        }
        return bot;
    }
}