package fr.ses10doigts.toolkitbridge.service.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminDetailResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminSummaryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentAdminFacadeTest {

    @Test
    void mapsListGetAndCreateOperations() {
        AgentAccountAdminService accountAdminService = mock(AgentAccountAdminService.class);
        AgentAccountService accountService = mock(AgentAccountService.class);
        AgentAdminFacade facade = new AgentAdminFacade(accountAdminService, accountService);

        AgentAccountAdminService.AgentAccountSummary summary = new AgentAccountAdminService.AgentAccountSummary(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "cortex",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        when(accountAdminService.listAgentAccounts()).thenReturn(List.of(summary));
        when(accountAdminService.getAgentAccount("cortex")).thenReturn(summary);
        when(accountService.createAgent("cortex")).thenReturn(new AgentProvisioningResult("cortex", "tb_xxx.yyy"));

        List<AgentAdminSummaryResponse> list = facade.listAgents();
        AgentAdminDetailResponse detail = facade.getAgent("cortex");
        AgentAdminCreateResponse created = facade.createAgent("cortex");

        assertThat(list).containsExactly(new AgentAdminSummaryResponse("cortex", true, Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(detail).isEqualTo(new AgentAdminDetailResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "cortex",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        ));
        assertThat(created).isEqualTo(new AgentAdminCreateResponse("cortex", "tb_xxx.yyy"));

        verify(accountAdminService).listAgentAccounts();
        verify(accountAdminService).getAgentAccount("cortex");
        verify(accountService).createAgent("cortex");
    }
}
