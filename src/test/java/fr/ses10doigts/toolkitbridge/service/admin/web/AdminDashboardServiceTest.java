package fr.ses10doigts.toolkitbridge.service.admin.web;

import fr.ses10doigts.toolkitbridge.model.dto.admin.agent.AgentAdminSummaryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AdminDashboardView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    @Test
    void buildsDashboardViewFromAvailableAdminData() {
        AgentAdminFacade agentAdminFacade = mock(AgentAdminFacade.class);
        TelegramBotAdminFacade telegramBotAdminFacade = mock(TelegramBotAdminFacade.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TechnicalAdminFacade technicalAdminFacade = mock(TechnicalAdminFacade.class);

        when(agentAdminFacade.listAgents())
                .thenReturn(List.of(new AgentAdminSummaryResponse("agent-a", true, Instant.now())));
        when(telegramBotAdminFacade.listTelegramBots())
                .thenReturn(List.of(
                        new TelegramBotAdminResponse("bot-a", true, 42L, false, true),
                        new TelegramBotAdminResponse("bot-b", true, 43L, false, true)
                ));
        when(llmAdminFacade.listLlms())
                .thenReturn(List.of(
                        new LlmAdminResponse("llm-a", "https://example.org", "gpt-5.4", true),
                        new LlmAdminResponse("llm-b", "https://example.org", "gpt-5.4-mini", true),
                        new LlmAdminResponse("llm-c", "https://example.org", "gpt-5.3", true)
                ));
        when(technicalAdminFacade.getOverview(8))
                .thenReturn(new TechnicalAdminView.Overview(
                        Instant.now(),
                        1,
                        0,
                        5,
                        0,
                        7,
                        3,
                        new TechnicalAdminView.ConfigItem(1, 3, List.of()),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ));

        AdminDashboardService service = new AdminDashboardService(
                agentAdminFacade,
                telegramBotAdminFacade,
                llmAdminFacade,
                technicalAdminFacade
        );

        AdminDashboardView view = service.buildView();

        assertThat(view.summaryCards()).hasSize(4);
        assertThat(view.shortcutLinks()).hasSize(4);
        assertThat(view.systemItems()).hasSize(5);

        AdminDashboardView.SummaryCard techniqueCard = view.summaryCards().stream()
                .filter(card -> card.title().equals("Technique"))
                .findFirst()
                .orElseThrow();
        assertThat(techniqueCard.value()).isEqualTo("Stable");
        assertThat(techniqueCard.toneClass()).isEqualTo("marcel-summary-card--green");

        AdminDashboardView.SystemItem llmSystemItem = view.systemItems().stream()
                .filter(item -> item.label().equals("LLM configures"))
                .findFirst()
                .orElseThrow();
        assertThat(llmSystemItem.value()).isEqualTo("3");
    }

    @Test
    void fallsBackToUnknownValuesWhenSourcesFail() {
        AgentAdminFacade agentAdminFacade = mock(AgentAdminFacade.class);
        TelegramBotAdminFacade telegramBotAdminFacade = mock(TelegramBotAdminFacade.class);
        LlmAdminFacade llmAdminFacade = mock(LlmAdminFacade.class);
        TechnicalAdminFacade technicalAdminFacade = mock(TechnicalAdminFacade.class);

        when(agentAdminFacade.listAgents()).thenThrow(new IllegalStateException("agent fail"));
        when(telegramBotAdminFacade.listTelegramBots()).thenThrow(new IllegalStateException("bot fail"));
        when(llmAdminFacade.listLlms()).thenThrow(new IllegalStateException("llm fail"));
        when(technicalAdminFacade.getOverview(8)).thenThrow(new IllegalStateException("tech fail"));

        AdminDashboardService service = new AdminDashboardService(
                agentAdminFacade,
                telegramBotAdminFacade,
                llmAdminFacade,
                technicalAdminFacade
        );

        AdminDashboardView view = service.buildView();

        AdminDashboardView.SummaryCard agentCard = view.summaryCards().stream()
                .filter(card -> card.title().equals("Agents"))
                .findFirst()
                .orElseThrow();
        assertThat(agentCard.value()).isEqualTo("--");

        AdminDashboardView.SummaryCard technicalCard = view.summaryCards().stream()
                .filter(card -> card.title().equals("Technique"))
                .findFirst()
                .orElseThrow();
        assertThat(technicalCard.value()).isEqualTo("Indisponible");
        assertThat(technicalCard.toneClass()).isEqualTo("marcel-summary-card--amber");

        AdminDashboardView.SystemItem traceItem = view.systemItems().stream()
                .filter(item -> item.label().equals("Traces recentes"))
                .findFirst()
                .orElseThrow();
        assertThat(traceItem.value()).isEqualTo("--");
    }
}
