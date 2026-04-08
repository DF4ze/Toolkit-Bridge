package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.exception.AgentNotFoundException;
import fr.ses10doigts.toolkitbridge.model.entity.AgentAccount;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AgentAccountAdminServiceTest {

    @Test
    void listAgentAccountsMapsRepositoryEntities() {
        AgentAccountRepository repository = mock(AgentAccountRepository.class);
        AgentAccountAdminService service = new AgentAccountAdminService(repository);

        AgentAccount account = new AgentAccount();
        account.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setAgentIdent("cortex");
        account.setEnabled(true);
        account.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(account));

        List<AgentAccountAdminService.AgentAccountSummary> summaries = service.listAgentAccounts();
        assertThat(summaries).containsExactly(new AgentAccountAdminService.AgentAccountSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "cortex",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        ));
    }

    @Test
    void getAgentAccountValidatesAndThrowsWhenMissing() {
        AgentAccountRepository repository = mock(AgentAccountRepository.class);
        AgentAccountAdminService service = new AgentAccountAdminService(repository);

        assertThatThrownBy(() -> service.getAgentAccount(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId cannot be empty");

        when(repository.findByAgentIdent("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAgentAccount("missing"))
                .isInstanceOf(AgentNotFoundException.class)
                .hasMessageContaining("missing");

        verify(repository).findByAgentIdent("missing");
    }
}
