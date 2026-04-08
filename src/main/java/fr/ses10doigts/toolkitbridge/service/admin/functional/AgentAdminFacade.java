package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminDetailResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminSummaryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentAdminFacade {

    private final AgentAccountAdminService agentAccountAdminService;
    private final AgentAccountService agentAccountService;

    public List<AgentAdminSummaryResponse> listAgents() {
        return agentAccountAdminService.listAgentAccounts().stream()
                .map(agent -> new AgentAdminSummaryResponse(
                        agent.agentId(),
                        agent.enabled(),
                        agent.createdAt()
                ))
                .toList();
    }

    public AgentAdminDetailResponse getAgent(String agentId) {
        AgentAccountAdminService.AgentAccountSummary summary = agentAccountAdminService.getAgentAccount(agentId);
        return new AgentAdminDetailResponse(
                summary.accountId(),
                summary.agentId(),
                summary.enabled(),
                summary.createdAt()
        );
    }

    public AgentAdminCreateResponse createAgent(String agentId) {
        AgentProvisioningResult result = agentAccountService.createAgent(agentId);
        return new AgentAdminCreateResponse(result.agentIdent(), result.apiKey());
    }
}
