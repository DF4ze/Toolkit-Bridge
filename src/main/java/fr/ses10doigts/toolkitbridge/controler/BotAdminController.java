package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.dto.auth.BotProvisioningResult;
import fr.ses10doigts.toolkitbridge.model.dto.auth.CreateBotRequest;
import fr.ses10doigts.toolkitbridge.service.auth.BotAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bots")
@RequiredArgsConstructor
public class BotAdminController {

    private final BotAccountService botAccountService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BotProvisioningResult createBot(@Valid @RequestBody CreateBotRequest request) {
        return botAccountService.createBot(request.getBotIdent());
    }
}