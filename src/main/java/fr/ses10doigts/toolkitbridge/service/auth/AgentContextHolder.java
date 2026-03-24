package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import org.springframework.stereotype.Component;

@Component
public class AgentContextHolder {

    private static final ThreadLocal<AuthenticatedAgent> CURRENT_BOT = new ThreadLocal<>();

    public void setCurrentBot(AuthenticatedAgent bot) {
        CURRENT_BOT.set(bot);
    }

    public AuthenticatedAgent getCurrentBot() {
        return CURRENT_BOT.get();
    }

    public void clear() {
        CURRENT_BOT.remove();
    }
}