package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram.TelegramSupervisionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TelegramBotAdminFacade {

    private final TelegramSupervisionProperties supervisionProperties;

    public List<TelegramBotAdminResponse> listTelegramBots() {
        if (supervisionProperties.getBotId() == null || supervisionProperties.getBotId().isBlank()) {
            return List.of();
        }
        return List.of(toResponse());
    }

    public Optional<TelegramBotAdminResponse> getTelegramBot(String botId) {
        if (botId == null || botId.isBlank()) {
            return Optional.empty();
        }

        return listTelegramBots().stream()
                .filter(bot -> bot.botId().equals(botId))
                .findFirst();
    }

    public TelegramBotAdminCreateResponse createTelegramBot(String botId) {
        return new TelegramBotAdminCreateResponse(
                "Telegram bot provisioning is not implemented yet. Requested botId=%s".formatted(botId)
        );
    }

    private TelegramBotAdminResponse toResponse() {
        return new TelegramBotAdminResponse(
                supervisionProperties.getBotId(),
                supervisionProperties.isEnabled(),
                supervisionProperties.getChatId(),
                supervisionProperties.isReadOnly(),
                supervisionProperties.getHumanIntervention().isEnabled()
        );
    }
}
