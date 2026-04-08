package fr.ses10doigts.toolkitbridge.controler.web.auth;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentAgentService currentAgentService;

    @GetMapping("/me")
    public AuthenticatedAgent getCurrentAgent() {
        return currentAgentService.getCurrentAgent();
    }
}
