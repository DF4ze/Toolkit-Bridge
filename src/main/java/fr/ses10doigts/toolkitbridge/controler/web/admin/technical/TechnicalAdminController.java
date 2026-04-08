package fr.ses10doigts.toolkitbridge.controler.web.admin.technical;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TechnicalAdminFacade;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/technical")
@RequiredArgsConstructor
public class TechnicalAdminController {

    private final TechnicalAdminFacade technicalAdminFacade;

    @GetMapping("/overview")
    public TechnicalAdminView.Overview overview(@RequestParam(required = false) Integer limit) {
        return technicalAdminFacade.getOverview(limit);
    }

    @GetMapping("/agents")
    public List<TechnicalAdminView.AgentItem> agents() {
        return technicalAdminFacade.listAgents();
    }

    @GetMapping("/tasks")
    public List<TechnicalAdminView.TaskItem> tasks(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) TaskStatus status
    ) {
        return technicalAdminFacade.listRecentTasks(limit, agentId, status);
    }

    @GetMapping("/traces")
    public List<TechnicalAdminView.TraceItem> traces(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId
    ) {
        return technicalAdminFacade.listRecentTraces(limit, agentId);
    }

    @GetMapping("/artifacts")
    public List<TechnicalAdminView.ArtifactItem> artifacts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String taskId
    ) {
        return technicalAdminFacade.listRecentArtifacts(limit, agentId, taskId);
    }

    @GetMapping("/configuration")
    public TechnicalAdminView.ConfigItem configuration() {
        return technicalAdminFacade.getConfigurationView();
    }

    @GetMapping("/retention")
    public List<TechnicalAdminView.RetentionItem> retention() {
        return technicalAdminFacade.listRetentionPolicies();
    }
}
