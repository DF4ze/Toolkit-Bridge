package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InMemoryAdminTaskStore implements AdminTaskStore {

    private final int maxEvents;
    private final LinkedHashMap<String, AdminTaskSnapshot> snapshotsByTaskId = new LinkedHashMap<>();

    public InMemoryAdminTaskStore(AdminTechnicalProperties properties) {
        this.maxEvents = Math.max(properties.getTasks().getMaxEvents(), 1);
    }

    public synchronized void record(Task task, String channelType, String conversationId, String errorMessage) {
        if (task == null) {
            return;
        }

        Instant now = Instant.now();
        AdminTaskSnapshot previous = snapshotsByTaskId.remove(task.taskId());
        Instant firstSeen = previous == null ? now : previous.firstSeenAt();

        AdminTaskSnapshot snapshot = new AdminTaskSnapshot(
                task.taskId(),
                task.parentTaskId(),
                task.objective(),
                task.initiator(),
                task.assignedAgentId(),
                task.traceId(),
                task.entryPoint(),
                task.status(),
                channelType,
                conversationId,
                firstSeen,
                now,
                normalize(errorMessage),
                task.artifacts() == null ? 0 : task.artifacts().size()
        );

        snapshotsByTaskId.put(task.taskId(), snapshot);
        evictOverflow();
    }

    public synchronized List<AdminTaskSnapshot> recent(int limit, String agentId, TaskStatus status) {
        int effectiveLimit = Math.max(limit, 1);
        List<AdminTaskSnapshot> values = new ArrayList<>(snapshotsByTaskId.values());
        List<AdminTaskSnapshot> filtered = new ArrayList<>();

        for (int i = values.size() - 1; i >= 0; i--) {
            AdminTaskSnapshot snapshot = values.get(i);
            if (!matchesAgent(snapshot, agentId)) {
                continue;
            }
            if (status != null && snapshot.status() != status) {
                continue;
            }
            filtered.add(snapshot);
            if (filtered.size() >= effectiveLimit) {
                break;
            }
        }
        return List.copyOf(filtered);
    }

    public synchronized int size() {
        return snapshotsByTaskId.size();
    }

    private void evictOverflow() {
        while (snapshotsByTaskId.size() > maxEvents) {
            String firstKey = snapshotsByTaskId.keySet().iterator().next();
            snapshotsByTaskId.remove(firstKey);
        }
    }

    private boolean matchesAgent(AdminTaskSnapshot snapshot, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return true;
        }
        return snapshot.assignedAgentId() != null
                && snapshot.assignedAgentId().trim().toLowerCase(Locale.ROOT)
                .equals(agentId.trim().toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
