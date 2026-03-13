package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class BotAuthController {

    private final CurrentBotService currentBotService;

    @GetMapping("/me")
    public AuthenticatedBot getCurrentBot() {
        return currentBotService.getCurrentBot();
    }
}