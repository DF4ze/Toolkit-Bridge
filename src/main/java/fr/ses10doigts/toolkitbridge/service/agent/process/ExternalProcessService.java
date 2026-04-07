package fr.ses10doigts.toolkitbridge.service.agent.process;

import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessHistoryEntry;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessUpdateRequest;
import fr.ses10doigts.toolkitbridge.service.agent.process.store.ExternalProcessStore;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExternalProcessService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)}}");

    private final ExternalProcessStore store;
    private final DefaultExternalProcessCatalog defaultCatalog;
    private final ObjectMapper objectMapper;
    private final CurrentAgentService currentAgentService;
    private final AgentPermissionControlService permissionControlService;

    public ExternalProcessService(ExternalProcessStore store,
                                  DefaultExternalProcessCatalog defaultCatalog,
                                  ObjectMapper objectMapper,
                                  CurrentAgentService currentAgentService,
                                  AgentPermissionControlService permissionControlService) {
        this.store = store;
        this.defaultCatalog = defaultCatalog;
        this.objectMapper = objectMapper;
        this.currentAgentService = currentAgentService;
        this.permissionControlService = permissionControlService;
    }

    public Optional<ExternalProcessSnapshot> findById(String processId) {
        Optional<ExternalProcessSnapshot> stored = store.findById(processId);
        if (stored.isPresent()) {
            return stored;
        }
        return defaultCatalog.findById(processId);
    }

    public ExternalProcessSnapshot updateProcess(ExternalProcessUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        permissionControlService.checkSharedWorkspaceWrite(
                currentAgentService.getCurrentAgent().agentIdent(),
                "update_external_process:" + request.processId()
        );
        return store.save(request);
    }

    public List<ExternalProcessHistoryEntry> listHistory(String processId) {
        return store.loadHistory(processId);
    }

    public <T> T loadTypedContent(String processId, Class<T> contentType) {
        ExternalProcessSnapshot snapshot = findById(processId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown external process: " + processId));
        try {
            return objectMapper.readValue(snapshot.content(), contentType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize external process content: " + processId, e);
        }
    }

    public String renderTemplate(String template, java.util.Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        String rendered = template;
        if (variables != null) {
            for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(rendered);
        if (matcher.find()) {
            throw new IllegalArgumentException("Missing template variable: " + matcher.group(1));
        }
        return rendered;
    }
}
