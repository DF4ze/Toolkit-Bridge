package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.model.dto.auth.CreateAgentRequest;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/agents")
@RequiredArgsConstructor
public class AgentAdminController {

    private final AgentAccountService agentAccountService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentProvisioningResult createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return agentAccountService.createBot(request.getAgentIdent());
    }

    @GetMapping("new")
    public AgentProvisioningResult newAgent(String agentIdent) {
        return agentAccountService.createBot(agentIdent);
    }
}