package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram.TelegramSupervisionProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotAdminFacadeTest {

    @Test
    void listsAndGetsConfiguredTelegramBot() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setBotId("bot-main");
        properties.setEnabled(true);
        properties.setChatId(123L);
        properties.setReadOnly(true);
        properties.getHumanIntervention().setEnabled(false);

        TelegramBotAdminFacade facade = new TelegramBotAdminFacade(properties);

        List<TelegramBotAdminResponse> bots = facade.listTelegramBots();
        assertThat(bots).containsExactly(new TelegramBotAdminResponse("bot-main", true, 123L, true, false));

        Optional<TelegramBotAdminResponse> bot = facade.getTelegramBot("bot-main");
        assertThat(bot).contains(new TelegramBotAdminResponse("bot-main", true, 123L, true, false));
        assertThat(facade.getTelegramBot("missing")).isEmpty();
    }

    @Test
    void returnsEmptyListWhenBotIdIsMissing() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setBotId("  ");

        TelegramBotAdminFacade facade = new TelegramBotAdminFacade(properties);

        assertThat(facade.listTelegramBots()).isEmpty();
    }
}
