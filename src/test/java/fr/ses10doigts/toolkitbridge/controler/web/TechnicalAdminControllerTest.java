package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicalAdminControllerTest {

    @Test
    void delegatesOverviewToFacade() {
        TechnicalAdminFacade facade = mock(TechnicalAdminFacade.class);
        TechnicalAdminController controller = new TechnicalAdminController(facade);

        TechnicalAdminView.Overview expected = new TechnicalAdminView.Overview(
                Instant.parse("2026-01-01T00:00:00Z"),
                1,
                0,
                0,
                0,
                0,
                0,
                new TechnicalAdminView.ConfigItem(0, 0, List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(facade.getOverview(25)).thenReturn(expected);

        TechnicalAdminView.Overview response = controller.overview(25);

        assertThat(response).isEqualTo(expected);
        verify(facade).getOverview(25);
    }

    @Test
    void delegatesListCallsToFacade() {
        TechnicalAdminFacade facade = mock(TechnicalAdminFacade.class);
        TechnicalAdminController controller = new TechnicalAdminController(facade);

        controller.agents();
        controller.tasks(10, "agent-1", TaskStatus.DONE);
        controller.traces(10, "agent-1");
        controller.artifacts(10, "agent-1", null);
        controller.configuration();
        controller.retention();

        verify(facade).listAgents();
        verify(facade).listRecentTasks(10, "agent-1", TaskStatus.DONE);
        verify(facade).listRecentTraces(10, "agent-1");
        verify(facade).listRecentArtifacts(10, "agent-1", null);
        verify(facade).getConfigurationView();
        verify(facade).listRetentionPolicies();
    }
}
