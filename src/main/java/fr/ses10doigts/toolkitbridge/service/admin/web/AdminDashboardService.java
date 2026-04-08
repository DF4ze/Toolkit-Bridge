package fr.ses10doigts.toolkitbridge.service.admin.web;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AdminDashboardView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.IntSupplier;

@Service
public class AdminDashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDashboardService.class);
    private static final int TECHNICAL_OVERVIEW_LIMIT = 8;
    private static final int UNKNOWN_COUNT = -1;
    private static final DateTimeFormatter DASHBOARD_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AgentAdminFacade agentAdminFacade;
    private final TelegramBotAdminFacade telegramBotAdminFacade;
    private final LlmAdminFacade llmAdminFacade;
    private final TechnicalAdminFacade technicalAdminFacade;

    public AdminDashboardService(
            AgentAdminFacade agentAdminFacade,
            TelegramBotAdminFacade telegramBotAdminFacade,
            LlmAdminFacade llmAdminFacade,
            TechnicalAdminFacade technicalAdminFacade
    ) {
        this.agentAdminFacade = agentAdminFacade;
        this.telegramBotAdminFacade = telegramBotAdminFacade;
        this.llmAdminFacade = llmAdminFacade;
        this.technicalAdminFacade = technicalAdminFacade;
    }

    public AdminDashboardView buildView() {
        TechnicalSnapshot technicalSnapshot = loadTechnicalSnapshot();
        int agentCount = safeCount(() -> agentAdminFacade.listAgents().size(), "agents");
        int telegramBotCount = safeCount(() -> telegramBotAdminFacade.listTelegramBots().size(), "telegramBots");
        int llmCount = safeCount(() -> llmAdminFacade.listLlms().size(), "llms");

        return new AdminDashboardView(
                buildSummaryCards(agentCount, telegramBotCount, technicalSnapshot),
                buildShortcutLinks(),
                buildSystemItems(technicalSnapshot, llmCount)
        );
    }

    private List<AdminDashboardView.SummaryCard> buildSummaryCards(
            int agentCount,
            int telegramBotCount,
            TechnicalSnapshot technicalSnapshot
    ) {
        String technicalValue = "Indisponible";
        String technicalDetail = "Facade technique non disponible";
        String technicalTone = "marcel-summary-card--amber";

        if (technicalSnapshot.hasData() && technicalSnapshot.recentErrors() == 0) {
            technicalValue = "Stable";
            technicalDetail = "Aucune erreur recente";
            technicalTone = "marcel-summary-card--green";
        } else if (technicalSnapshot.hasData()) {
            technicalValue = "A surveiller";
            technicalDetail = formatCount(technicalSnapshot.recentErrors()) + " erreurs recentes";
        }

        return List.of(
                new AdminDashboardView.SummaryCard("Agents", formatCount(agentCount), "Comptes agents disponibles", "marcel-summary-card--blue", "/admin/agents"),
                new AdminDashboardView.SummaryCard("Bots Telegram", formatCount(telegramBotCount), "Canaux Telegram relies", "marcel-summary-card--gold", "/admin/telegram-bots"),
                new AdminDashboardView.SummaryCard("Taches recentes", formatCount(technicalSnapshot.recentTasks()), "Activite technique recente", "marcel-summary-card--blue", "/admin/technical"),
                new AdminDashboardView.SummaryCard("Technique", technicalValue, technicalDetail, technicalTone, "/admin/technical")
        );
    }

    private List<AdminDashboardView.ShortcutLink> buildShortcutLinks() {
        return List.of(
                new AdminDashboardView.ShortcutLink("LLM", "Configurer providers et modeles", "/admin/llms"),
                new AdminDashboardView.ShortcutLink("Bots Telegram", "Piloter la supervision Telegram", "/admin/telegram-bots"),
                new AdminDashboardView.ShortcutLink("Agents", "Explorer et preparer la gestion des agents", "/admin/agents"),
                new AdminDashboardView.ShortcutLink("Technique", "Supervision, traces et diagnostics", "/admin/technical")
        );
    }

    private List<AdminDashboardView.SystemItem> buildSystemItems(TechnicalSnapshot technicalSnapshot, int llmCount) {
        String generatedAt = LocalDateTime.now().format(DASHBOARD_TIME_FORMAT);

        return List.of(
                new AdminDashboardView.SystemItem("Socle UI", "Marcel admin layout actif"),
                new AdminDashboardView.SystemItem("Derniere verification", generatedAt),
                new AdminDashboardView.SystemItem("LLM configures", formatCount(llmCount)),
                new AdminDashboardView.SystemItem("Traces recentes", formatCount(technicalSnapshot.recentTraces())),
                new AdminDashboardView.SystemItem("Artefacts recents", formatCount(technicalSnapshot.recentArtifacts()))
        );
    }

    private TechnicalSnapshot loadTechnicalSnapshot() {
        try {
            TechnicalAdminView.Overview overview = technicalAdminFacade.getOverview(TECHNICAL_OVERVIEW_LIMIT);
            return new TechnicalSnapshot(
                    overview.recentTasks(),
                    overview.recentErrors(),
                    overview.recentTraces(),
                    overview.recentArtifacts(),
                    true
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to load technical overview for admin dashboard: {}", exception.getMessage());
            LOGGER.debug("Technical overview loading failure", exception);
            return TechnicalSnapshot.unknown();
        }
    }

    private int safeCount(IntSupplier supplier, String metricName) {
        try {
            return supplier.getAsInt();
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to load '{}' metric for admin dashboard: {}", metricName, exception.getMessage());
            LOGGER.debug("Admin dashboard metric loading failure for {}", metricName, exception);
            return UNKNOWN_COUNT;
        }
    }

    private String formatCount(int count) {
        return count < 0 ? "--" : Integer.toString(count);
    }

    private record TechnicalSnapshot(
            int recentTasks,
            int recentErrors,
            int recentTraces,
            int recentArtifacts,
            boolean hasData
    ) {
        private static TechnicalSnapshot unknown() {
            return new TechnicalSnapshot(UNKNOWN_COUNT, UNKNOWN_COUNT, UNKNOWN_COUNT, UNKNOWN_COUNT, false);
        }
    }
}
