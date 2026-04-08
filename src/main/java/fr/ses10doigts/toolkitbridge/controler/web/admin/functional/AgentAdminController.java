package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateRequest;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminDetailResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminSummaryResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentAdminFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/agents")
@RequiredArgsConstructor
public class AgentAdminController {

    private final AgentAdminFacade agentAdminFacade;

    @GetMapping
    public List<AgentAdminSummaryResponse> listAgents() {
        return agentAdminFacade.listAgents();
    }

    @GetMapping("/{agentId}")
    public AgentAdminDetailResponse getAgent(@PathVariable String agentId) {
        return agentAdminFacade.getAgent(agentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentAdminCreateResponse createAgent(@Valid @RequestBody AgentAdminCreateRequest request) {
        return agentAdminFacade.createAgent(request.agentId());
    }
}
