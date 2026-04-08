package fr.ses10doigts.toolkitbridge.controler.web.admin.technical;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class TechnicalAdminPageControllerTest {

    private TechnicalAdminFacade technicalAdminFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        technicalAdminFacade = mock(TechnicalAdminFacade.class);

        AdminTechnicalProperties technicalProperties = new AdminTechnicalProperties();
        technicalProperties.setDefaultListLimit(25);
        technicalProperties.setMaxListLimit(100);

        TechnicalAdminPageController controller = new TechnicalAdminPageController(technicalAdminFacade, technicalProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(technicalAdminFacade.listAgents()).thenReturn(List.of(
                new TechnicalAdminView.AgentItem(
                        "agent-1",
                        "Agent One",
                        "WORKER",
                        "TASK",
                        "openai",
                        "gpt",
                        "default",
                        true,
                        null,
                        null,
                        List.of("read_file")
                )
        ));
    }

    @Test
    void servesTechnicalOverviewPage() throws Exception {
        when(technicalAdminFacade.getOverview(eq(25))).thenReturn(new TechnicalAdminView.Overview(
                Instant.parse("2026-01-01T00:00:00Z"),
                1,
                0,
                1,
                0,
                1,
                1,
                new TechnicalAdminView.ConfigItem(1, 1, List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/admin/technical"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/overview"))
                .andExpect(model().attribute("activeNav", "technical"))
                .andExpect(model().attribute("activeTechnicalNav", "overview"))
                .andExpect(model().attribute("effectiveLimit", 25));

        verify(technicalAdminFacade).getOverview(25);
    }

    @Test
    void servesTechnicalAgentsPage() throws Exception {
        mockMvc.perform(get("/admin/technical/agents"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/agents"))
                .andExpect(model().attribute("activeTechnicalNav", "agents"))
                .andExpect(model().attribute("agents", hasSize(1)));

        verify(technicalAdminFacade).listAgents();
    }

    @Test
    void servesTechnicalTasksPageWithFilters() throws Exception {
        when(technicalAdminFacade.listRecentTasks(12, "agent-1", TaskStatus.RUNNING)).thenReturn(List.of());

        mockMvc.perform(get("/admin/technical/tasks")
                        .param("limit", "12")
                        .param("agentId", "agent-1")
                        .param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/tasks"))
                .andExpect(model().attribute("activeTechnicalNav", "tasks"))
                .andExpect(model().attribute("selectedAgentId", "agent-1"))
                .andExpect(model().attribute("selectedStatus", TaskStatus.RUNNING))
                .andExpect(model().attribute("effectiveLimit", 12));

        verify(technicalAdminFacade).listRecentTasks(12, "agent-1", TaskStatus.RUNNING);
    }

    @Test
    void normalizesBlankFiltersAndAppliesConfiguredLimitBounds() throws Exception {
        when(technicalAdminFacade.listRecentTasks(100, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/admin/technical/tasks")
                        .param("limit", "999")
                        .param("agentId", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/tasks"))
                .andExpect(model().attribute("selectedAgentId", (Object) null))
                .andExpect(model().attribute("selectedStatus", (Object) null))
                .andExpect(model().attribute("effectiveLimit", 100));

        verify(technicalAdminFacade).listRecentTasks(100, null, null);
        verify(technicalAdminFacade, times(1)).listAgents();
    }

    @Test
    void servesTechnicalTracesAndArtifactsPagesWithFilters() throws Exception {
        when(technicalAdminFacade.listRecentTraces(30, "agent-1")).thenReturn(List.of());
        when(technicalAdminFacade.listRecentArtifacts(40, "agent-1", "task-7")).thenReturn(List.of());

        mockMvc.perform(get("/admin/technical/traces")
                        .param("limit", "30")
                        .param("agentId", "agent-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/traces"))
                .andExpect(model().attribute("activeTechnicalNav", "traces"))
                .andExpect(model().attribute("selectedAgentId", "agent-1"));

        mockMvc.perform(get("/admin/technical/artifacts")
                        .param("limit", "40")
                        .param("agentId", "agent-1")
                        .param("taskId", "task-7"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/artifacts"))
                .andExpect(model().attribute("activeTechnicalNav", "artifacts"))
                .andExpect(model().attribute("selectedTaskId", "task-7"));

        verify(technicalAdminFacade).listRecentTraces(30, "agent-1");
        verify(technicalAdminFacade).listRecentArtifacts(40, "agent-1", "task-7");
    }

    @Test
    void servesConfigurationAndRetentionPages() throws Exception {
        when(technicalAdminFacade.getConfigurationView()).thenReturn(new TechnicalAdminView.ConfigItem(1, 1, List.of()));
        when(technicalAdminFacade.listRetentionPolicies()).thenReturn(List.of());

        mockMvc.perform(get("/admin/technical/configuration"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/configuration"))
                .andExpect(model().attribute("activeTechnicalNav", "configuration"));

        mockMvc.perform(get("/admin/technical/retention"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/technical/retention"))
                .andExpect(model().attribute("activeTechnicalNav", "retention"));

        verify(technicalAdminFacade).getConfigurationView();
        verify(technicalAdminFacade).listRetentionPolicies();
    }
}
