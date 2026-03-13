package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import org.springframework.stereotype.Component;

@Component
public class BotContextHolder {

    private static final ThreadLocal<AuthenticatedBot> CURRENT_BOT = new ThreadLocal<>();

    public void setCurrentBot(AuthenticatedBot bot) {
        CURRENT_BOT.set(bot);
    }

    public AuthenticatedBot getCurrentBot() {
        return CURRENT_BOT.get();
    }

    public void clear() {
        CURRENT_BOT.remove();
    }
}