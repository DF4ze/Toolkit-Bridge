package fr.ses10doigts.toolkitbridge.controler.web.admin.telegram;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class TelegramBotAdminPageControllerTest {

    private TelegramBotAdminFacade telegramBotAdminFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        telegramBotAdminFacade = mock(TelegramBotAdminFacade.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TelegramBotAdminPageController(telegramBotAdminFacade)).build();
    }

    @Test
    void servesTelegramBotListPage() throws Exception {
        TelegramBotAdminResponse bot = new TelegramBotAdminResponse("bot-main", true, 123L, true, true);
        when(telegramBotAdminFacade.listTelegramBots()).thenReturn(List.of(bot));

        mockMvc.perform(get("/admin/telegram-bots"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/telegram-bots/list"))
                .andExpect(model().attribute("activeNav", "telegram-bots"))
                .andExpect(model().attribute("activeTelegramBotNav", "list"))
                .andExpect(model().attribute("bots", hasSize(1)));

        verify(telegramBotAdminFacade).listTelegramBots();
    }

    @Test
    void servesTelegramBotDetailOrRedirectsWhenMissing() throws Exception {
        TelegramBotAdminResponse bot = new TelegramBotAdminResponse("bot-main", true, 123L, true, true);
        when(telegramBotAdminFacade.getTelegramBot("bot-main")).thenReturn(Optional.of(bot));
        when(telegramBotAdminFacade.getTelegramBot("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/telegram-bots/bot-main"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/telegram-bots/detail"))
                .andExpect(model().attribute("bot", bot));

        mockMvc.perform(get("/admin/telegram-bots/missing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/telegram-bots"));

        verify(telegramBotAdminFacade).getTelegramBot("bot-main");
        verify(telegramBotAdminFacade).getTelegramBot("missing");
    }
}
