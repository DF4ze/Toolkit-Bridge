package fr.ses10doigts.toolkitbridge.model.dto.admin.technical;

import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class TechnicalAdminView {

    private TechnicalAdminView() {
    }

    public record AgentItem(
            String agentId,
            String name,
            String role,
            String orchestrator,
            String provider,
            String model,
            String policyName,
            boolean toolsEnabled,
            RuntimeItem runtime,
            PolicyItem policy,
            List<String> exposedTools
    ) {
    }

    public record RuntimeItem(
            String availability,
            boolean busy,
            String currentTask,
            String activeContext,
            String traceId,
            String channelType,
            String channelConversationId,
            Instant startedAt,
            Instant updatedAt
    ) {
    }

    public record PolicyItem(
            String name,
            Set<String> allowedTools,
            Set<String> accessibleMemoryScopes,
            boolean delegationAllowed,
            boolean webAccessAllowed,
            boolean sharedWorkspaceWriteAllowed,
            boolean scriptedToolExecutionAllowed
    ) {
    }

    public record TaskItem(
            String taskId,
            String parentTaskId,
            String objective,
            String initiator,
            String assignedAgentId,
            String traceId,
            String entryPoint,
            TaskStatus status,
            String channelType,
            String conversationId,
            Instant firstSeenAt,
            Instant lastSeenAt,
            String errorMessage,
            int artifactCount
    ) {
    }

    public record TraceItem(
            Instant occurredAt,
            AgentTraceEventType type,
            String source,
            String runId,
            String agentId,
            String messageId,
            String taskId,
            java.util.Map<String, Object> attributes
    ) {
    }

    public record ArtifactItem(
            String artifactId,
            String type,
            String taskId,
            String producerAgentId,
            String title,
            Instant createdAt,
            Instant expiresAt,
            String storageKind,
            String location,
            String mediaType,
            long sizeBytes
    ) {
    }

    public record ConfigItem(
            int agentDefinitionCount,
            int llmProviderCount,
            List<LlmProviderItem> llmProviders
    ) {
    }

    public record LlmProviderItem(
            String name,
            String baseUrl,
            String defaultModel,
            boolean apiKeyConfigured
    ) {
    }

    public record RetentionItem(
            String family,
            Duration ttl,
            String disposition
    ) {
    }

    public record Overview(
            Instant generatedAt,
            int agents,
            int busyAgents,
            int recentTasks,
            int recentErrors,
            int recentTraces,
            int recentArtifacts,
            ConfigItem configuration,
            List<RetentionItem> retention,
            List<AgentItem> agentStates,
            List<TaskItem> tasks,
            List<TraceItem> traces,
            List<ArtifactItem> artifacts
    ) {
    }
}
