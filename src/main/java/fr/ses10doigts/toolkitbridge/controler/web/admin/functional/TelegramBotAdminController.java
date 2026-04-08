package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminCreateRequest;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/telegram-bots")
@RequiredArgsConstructor
public class TelegramBotAdminController {

    private final TelegramBotAdminFacade telegramBotAdminFacade;

    @GetMapping
    public List<TelegramBotAdminResponse> listTelegramBots() {
        return telegramBotAdminFacade.listTelegramBots();
    }

    @GetMapping("/{botId}")
    public ResponseEntity<TelegramBotAdminResponse> getTelegramBot(@PathVariable String botId) {
        return ResponseEntity.of(telegramBotAdminFacade.getTelegramBot(botId));
    }

    @PostMapping
    public ResponseEntity<TelegramBotAdminCreateResponse> createTelegramBot(
            @Valid @RequestBody TelegramBotAdminCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(telegramBotAdminFacade.createTelegramBot(request.botId()));
    }
}
