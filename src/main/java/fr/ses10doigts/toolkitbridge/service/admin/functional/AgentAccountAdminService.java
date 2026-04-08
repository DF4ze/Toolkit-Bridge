package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.exception.AgentNotFoundException;
import fr.ses10doigts.toolkitbridge.model.entity.AgentAccount;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentAccountAdminService {

    private static final String AGENT_ID_PATTERN = "[a-zA-Z0-9._-]+";

    private final AgentAccountRepository agentAccountRepository;

    @Transactional(readOnly = true)
    public List<AgentAccountSummary> listAgentAccounts() {
        return agentAccountRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentAccountSummary getAgentAccount(String agentId) {
        validateAgentId(agentId);

        AgentAccount agentAccount = agentAccountRepository.findByAgentIdent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        return toSummary(agentAccount);
    }

    private void validateAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId cannot be empty");
        }
        if (agentId.length() > 100) {
            throw new IllegalArgumentException("agentId is too long");
        }
        if (!agentId.matches(AGENT_ID_PATTERN)) {
            throw new IllegalArgumentException("agentId contains forbidden characters");
        }
    }

    private AgentAccountSummary toSummary(AgentAccount account) {
        return new AgentAccountSummary(
                account.getId(),
                account.getAgentIdent(),
                account.isEnabled(),
                account.getCreatedAt()
        );
    }

    public record AgentAccountSummary(
            UUID accountId,
            String agentId,
            boolean enabled,
            Instant createdAt
    ) {
    }
}
