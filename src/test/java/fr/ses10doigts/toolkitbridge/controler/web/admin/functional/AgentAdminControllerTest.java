package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateRequest;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminDetailResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminSummaryResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentAdminFacade;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentAdminControllerTest {

    @Test
    void delegatesListGetAndCreateToFacade() {
        AgentAdminFacade facade = mock(AgentAdminFacade.class);
        AgentAdminController controller = new AgentAdminController(facade);

        List<AgentAdminSummaryResponse> summaries = List.of(
                new AgentAdminSummaryResponse("cortex", true, Instant.parse("2026-01-01T00:00:00Z"))
        );
        AgentAdminDetailResponse detail = new AgentAdminDetailResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "cortex",
                true,
                Instant.parse("2026-01-01T00:00:00Z")
        );
        AgentAdminCreateResponse created = new AgentAdminCreateResponse("cortex", "tb_xxx.yyy");

        when(facade.listAgents()).thenReturn(summaries);
        when(facade.getAgent("cortex")).thenReturn(detail);
        when(facade.createAgent("cortex")).thenReturn(created);

        assertThat(controller.listAgents()).isEqualTo(summaries);
        assertThat(controller.getAgent("cortex")).isEqualTo(detail);
        assertThat(controller.createAgent(new AgentAdminCreateRequest("cortex"))).isEqualTo(created);

        verify(facade).listAgents();
        verify(facade).getAgent("cortex");
        verify(facade).createAgent("cortex");
    }
}
