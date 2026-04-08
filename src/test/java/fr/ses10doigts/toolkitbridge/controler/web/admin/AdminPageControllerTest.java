package fr.ses10doigts.toolkitbridge.controler.web.admin;

import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AdminDashboardView;
import fr.ses10doigts.toolkitbridge.service.admin.web.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.mockito.Mockito.*;

class AdminPageControllerTest {

    private AdminDashboardService adminDashboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminDashboardService = mock(AdminDashboardService.class);
        when(adminDashboardService.buildView())
                .thenReturn(new AdminDashboardView(
                        List.of(
                                new AdminDashboardView.SummaryCard("Agents", "4", "desc", "marcel-summary-card--blue", "/admin/agents"),
                                new AdminDashboardView.SummaryCard("Bots Telegram", "1", "desc", "marcel-summary-card--gold", "/admin/telegram-bots"),
                                new AdminDashboardView.SummaryCard("Taches recentes", "9", "desc", "marcel-summary-card--blue", "/admin/technical"),
                                new AdminDashboardView.SummaryCard("Technique", "Stable", "desc", "marcel-summary-card--green", "/admin/technical")
                        ),
                        List.of(
                                new AdminDashboardView.ShortcutLink("LLM", "desc", "/admin/llms"),
                                new AdminDashboardView.ShortcutLink("Bots Telegram", "desc", "/admin/telegram-bots"),
                                new AdminDashboardView.ShortcutLink("Agents", "desc", "/admin/agents"),
                                new AdminDashboardView.ShortcutLink("Technique", "desc", "/admin/technical")
                        ),
                        List.of(
                                new AdminDashboardView.SystemItem("Socle UI", "OK"),
                                new AdminDashboardView.SystemItem("Derniere verification", "2026-04-08 17:00"),
                                new AdminDashboardView.SystemItem("LLM configures", "2"),
                                new AdminDashboardView.SystemItem("Traces recentes", "6"),
                                new AdminDashboardView.SystemItem("Artefacts recents", "3")
                        )
                ));

        AdminPageController controller = new AdminPageController(adminDashboardService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void servesDashboardWithExpectedModel() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("activeNav", "dashboard"))
                .andExpect(model().attribute("summaryCards", hasSize(4)))
                .andExpect(model().attribute("shortcutLinks", hasSize(4)))
                .andExpect(model().attribute("systemItems", hasSize(5)));
    }

    @Test
    void servesUiPreviewPageWithDashboardNavContext() throws Exception {
        mockMvc.perform(get("/admin/ui-preview"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ui-preview"))
                .andExpect(model().attribute("activeNav", "dashboard"));
    }
}
