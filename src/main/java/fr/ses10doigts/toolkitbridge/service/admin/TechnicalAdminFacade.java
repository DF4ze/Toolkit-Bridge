package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TechnicalAdminFacade {

    private final AdminAgentQueryService adminAgentQueryService;
    private final AdminTaskQueryService adminTaskQueryService;
    private final TraceQueryService traceQueryService;
    private final ArtifactQueryService artifactQueryService;
    private final AdminConfigQueryService adminConfigQueryService;
    private final RetentionQueryService retentionQueryService;
    private final AdminTechnicalProperties technicalProperties;

    public TechnicalAdminFacade(
            AdminAgentQueryService adminAgentQueryService,
            AdminTaskQueryService adminTaskQueryService,
            TraceQueryService traceQueryService,
            ArtifactQueryService artifactQueryService,
            AdminConfigQueryService adminConfigQueryService,
            RetentionQueryService retentionQueryService,
            AdminTechnicalProperties technicalProperties
    ) {
        this.adminAgentQueryService = adminAgentQueryService;
        this.adminTaskQueryService = adminTaskQueryService;
        this.traceQueryService = traceQueryService;
        this.artifactQueryService = artifactQueryService;
        this.adminConfigQueryService = adminConfigQueryService;
        this.retentionQueryService = retentionQueryService;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.AgentItem> listAgents() {
        return adminAgentQueryService.listAgents();
    }

    public List<TechnicalAdminView.TaskItem> listRecentTasks(Integer limit, String agentId, TaskStatus status) {
        return adminTaskQueryService.listRecentTasks(limit, agentId, status);
    }

    public List<TechnicalAdminView.TraceItem> listRecentTraces(Integer limit, String agentId) {
        return traceQueryService.listRecentTraces(limit, agentId);
    }

    public List<TechnicalAdminView.ArtifactItem> listRecentArtifacts(Integer limit, String agentId, String taskId) {
        return artifactQueryService.listRecentArtifacts(limit, agentId, taskId);
    }

    public TechnicalAdminView.ConfigItem getConfigurationView() {
        return adminConfigQueryService.getConfigurationView();
    }

    public List<TechnicalAdminView.RetentionItem> listRetentionPolicies() {
        return retentionQueryService.listRetentionPolicies();
    }

    public TechnicalAdminView.Overview getOverview(Integer limit) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<TechnicalAdminView.AgentItem> agents = listAgents();
        List<TechnicalAdminView.TaskItem> tasks = listRecentTasks(effectiveLimit, null, null);
        List<TechnicalAdminView.TraceItem> traces = listRecentTraces(effectiveLimit, null);
        List<TechnicalAdminView.ArtifactItem> artifacts = listRecentArtifacts(effectiveLimit, null, null);

        int busyAgents = (int) agents.stream()
                .filter(agent -> agent.runtime() != null && agent.runtime().busy())
                .count();

        int recentErrors = (int) traces.stream()
                .filter(trace -> trace.type() == AgentTraceEventType.ERROR)
                .count();

        return new TechnicalAdminView.Overview(
                Instant.now(),
                agents.size(),
                busyAgents,
                tasks.size(),
                recentErrors,
                traces.size(),
                artifacts.size(),
                getConfigurationView(),
                listRetentionPolicies(),
                agents,
                tasks,
                traces,
                artifacts
        );
    }
}
