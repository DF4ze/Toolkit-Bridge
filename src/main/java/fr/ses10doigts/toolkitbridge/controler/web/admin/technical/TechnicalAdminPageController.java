package fr.ses10doigts.toolkitbridge.controler.web.admin.technical;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/technical")
@RequiredArgsConstructor
public class TechnicalAdminPageController {

    private final TechnicalAdminFacade technicalAdminFacade;
    private final AdminTechnicalProperties technicalProperties;

    @GetMapping({"", "/"})
    public String overview(
            @RequestParam(required = false) Integer limit,
            Model model
    ) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        TechnicalAdminView.Overview overview = technicalAdminFacade.getOverview(effectiveLimit);

        setTechnicalNavigation(model, "overview");
        model.addAttribute("effectiveLimit", effectiveLimit);
        model.addAttribute("overview", overview);
        return "admin/technical/overview";
    }

    @GetMapping("/agents")
    public String agents(Model model) {
        List<TechnicalAdminView.AgentItem> agents = sortedAgents();

        setTechnicalNavigation(model, "agents");
        model.addAttribute("agents", agents);
        return "admin/technical/agents";
    }

    @GetMapping("/tasks")
    public String tasks(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) TaskStatus status,
            Model model
    ) {
        String normalizedAgentId = normalize(agentId);
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<TechnicalAdminView.TaskItem> tasks = technicalAdminFacade.listRecentTasks(effectiveLimit, normalizedAgentId, status);

        setTechnicalNavigation(model, "tasks");
        setFilterMetadata(model, effectiveLimit);
        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedAgentId", normalizedAgentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statusOptions", TaskStatus.values());
        addAgentOptions(model);
        return "admin/technical/tasks";
    }

    @GetMapping("/traces")
    public String traces(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId,
            Model model
    ) {
        String normalizedAgentId = normalize(agentId);
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<TechnicalAdminView.TraceItem> traces = technicalAdminFacade.listRecentTraces(effectiveLimit, normalizedAgentId);

        setTechnicalNavigation(model, "traces");
        setFilterMetadata(model, effectiveLimit);
        model.addAttribute("traces", traces);
        model.addAttribute("selectedAgentId", normalizedAgentId);
        addAgentOptions(model);
        return "admin/technical/traces";
    }

    @GetMapping("/artifacts")
    public String artifacts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String taskId,
            Model model
    ) {
        String normalizedAgentId = normalize(agentId);
        String normalizedTaskId = normalize(taskId);
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<TechnicalAdminView.ArtifactItem> artifacts = technicalAdminFacade.listRecentArtifacts(
                effectiveLimit,
                normalizedAgentId,
                normalizedTaskId
        );

        setTechnicalNavigation(model, "artifacts");
        setFilterMetadata(model, effectiveLimit);
        model.addAttribute("artifacts", artifacts);
        model.addAttribute("selectedAgentId", normalizedAgentId);
        model.addAttribute("selectedTaskId", normalizedTaskId);
        addAgentOptions(model);
        return "admin/technical/artifacts";
    }

    @GetMapping("/configuration")
    public String configuration(Model model) {
        TechnicalAdminView.ConfigItem configuration = technicalAdminFacade.getConfigurationView();

        setTechnicalNavigation(model, "configuration");
        model.addAttribute("configuration", configuration);
        return "admin/technical/configuration";
    }

    @GetMapping("/retention")
    public String retention(Model model) {
        List<TechnicalAdminView.RetentionItem> retentionItems = technicalAdminFacade.listRetentionPolicies();

        setTechnicalNavigation(model, "retention");
        model.addAttribute("retentionItems", retentionItems);
        return "admin/technical/retention";
    }

    private void setTechnicalNavigation(Model model, String activeTechnicalNav) {
        model.addAttribute("activeNav", "technical");
        model.addAttribute("activeTechnicalNav", activeTechnicalNav);
    }

    private void setFilterMetadata(Model model, int effectiveLimit) {
        model.addAttribute("effectiveLimit", effectiveLimit);
    }

    private void addAgentOptions(Model model) {
        model.addAttribute("agentOptions", sortedAgents());
    }

    private List<TechnicalAdminView.AgentItem> sortedAgents() {
        return technicalAdminFacade.listAgents().stream()
                .sorted(Comparator.comparing(TechnicalAdminView.AgentItem::agentId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
