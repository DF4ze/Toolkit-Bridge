package fr.ses10doigts.toolkitbridge.controler.web.admin.telegram;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/telegram-bots")
@RequiredArgsConstructor
public class TelegramBotAdminPageController {

    private final TelegramBotAdminFacade telegramBotAdminFacade;

    @GetMapping
    public String list(Model model) {
        List<TelegramBotAdminResponse> bots = telegramBotAdminFacade.listTelegramBots();
        applyNavigation(model);
        model.addAttribute("bots", bots);
        return "admin/telegram-bots/list";
    }

    @GetMapping("/{botId}")
    public String detail(@PathVariable String botId, Model model) {
        return telegramBotAdminFacade.getTelegramBot(botId)
                .map(bot -> {
                    applyNavigation(model);
                    model.addAttribute("bot", bot);
                    return "admin/telegram-bots/detail";
                })
                .orElse("redirect:/admin/telegram-bots");
    }

    private void applyNavigation(Model model) {
        model.addAttribute("activeNav", "telegram-bots");
        model.addAttribute("activeTelegramBotNav", "list");
    }
}
