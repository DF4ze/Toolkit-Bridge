package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.telegrambots.service.sender.TelegramSender;
import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class TelegramSupervisionMessagePublisher {

    private final TelegramSupervisionProperties properties;
    private final Optional<TelegramSenderRegistry> senderRegistry;
    private final boolean telegramEnabled;

    public TelegramSupervisionMessagePublisher(TelegramSupervisionProperties properties,
                                               Optional<TelegramSenderRegistry> senderRegistry,
                                               @Value("${telegram.enabled:true}") boolean telegramEnabled) {
        this.properties = properties;
        this.senderRegistry = senderRegistry;
        this.telegramEnabled = telegramEnabled;
    }

    @PostConstruct
    void warnWhenTelegramSupervisionCannotStart() {
        if (!telegramEnabled) {
            log.warn("telegram.enabled=false: Telegram support is disabled, so Telegram supervision cannot be active.");
            return;
        }
        if (properties.isEnabled() && telegramEnabled && senderRegistry.isEmpty()) {
            log.warn("Telegram supervision is enabled, but no TelegramSenderRegistry bean is available. Supervision messages will not be published.");
        }
    }

    public boolean isConfigured() {
        return properties.hasTargetChat() && telegramEnabled && senderRegistry.isPresent();
    }

    public void publish(String message) {
        if (!isConfigured() || message == null || message.isBlank()) {
            return;
        }

        try {
            resolveSender().sendMessage(properties.getChatId(), truncate(message));
        } catch (RuntimeException e) {
            log.warn("Failed to publish supervision message to Telegram chatId={}", properties.getChatId(), e);
        }
    }

    private TelegramSender resolveSender() {
        TelegramSenderRegistry registry = senderRegistry
                .orElseThrow(() -> new IllegalStateException("Telegram sender registry is unavailable"));
        if (properties.getBotId() == null) {
            return registry.getDefaultBotSender();
        }
        return registry.getRequiredSender(properties.getBotId());
    }

    private String truncate(String message) {
        int maxLength = Math.max(properties.getMaxMessageLength(), 80);
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
    }
}
