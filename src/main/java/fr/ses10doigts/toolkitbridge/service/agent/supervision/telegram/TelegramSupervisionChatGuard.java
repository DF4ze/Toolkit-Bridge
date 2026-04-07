package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import org.springframework.stereotype.Component;

@Component
public class TelegramSupervisionChatGuard {

    private final TelegramSupervisionProperties properties;

    public TelegramSupervisionChatGuard(TelegramSupervisionProperties properties) {
        this.properties = properties;
    }

    public boolean isReadOnlySupervisionChat(String botId, Long chatId) {
        if (!properties.hasTargetChat() || !properties.isReadOnly()) {
            return false;
        }
        if (chatId == null || !chatId.equals(properties.getChatId())) {
            return false;
        }
        String configuredBotId = properties.getBotId();
        if (configuredBotId == null) {
            return true;
        }
        return configuredBotId.equals(botId);
    }
}
