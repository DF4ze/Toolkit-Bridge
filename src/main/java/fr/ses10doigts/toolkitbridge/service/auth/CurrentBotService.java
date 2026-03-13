package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.exception.InvalidApiKeyException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import org.springframework.stereotype.Service;

@Service
public class CurrentBotService {

    private final BotContextHolder botContextHolder;

    public CurrentBotService(BotContextHolder botContextHolder) {
        this.botContextHolder = botContextHolder;
    }

    public AuthenticatedBot getCurrentBot() {
        AuthenticatedBot bot = botContextHolder.getCurrentBot();
        if (bot == null) {
            throw new InvalidApiKeyException("Authentication is required");
        }
        return bot;
    }
}