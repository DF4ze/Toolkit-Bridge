package fr.ses10doigts.toolkitbridge.persistence.model;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessagePayload;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessageType;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.task.TaskPrompt;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceTypingTest {

    @Test
    void marksCorePersistableFamiliesAsDurable() {
        Task task = new Task(
                "task-1",
                "Ship release",
                "user",
                "agent-1",
                null,
                "trace-1",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                TaskStatus.CREATED,
                Map.of(),
                List.of()
        );
        AgentMessage message = new AgentMessage(
                "message-1",
                "corr-1",
                "agent-1",
                AgentRecipient.forAgent("agent-2"),
                Instant.parse("2026-04-07T12:00:00Z"),
                AgentMessageType.TASK_REQUEST,
                new AgentMessagePayload("hello", "internal", "u1", "c1", "p1", Map.of(), List.of())
        );
        AgentTraceEvent traceEvent = new AgentTraceEvent(
                Instant.parse("2026-04-07T12:00:00Z"),
                AgentTraceEventType.DELEGATION,
                "message_bus",
                null,
                Map.of()
        );
        Artifact artifact = new Artifact(
                "artifact-1",
                ArtifactType.REPORT,
                "task-1",
                "agent-1",
                "Release report",
                Instant.parse("2026-04-07T12:00:00Z"),
                Instant.parse("2026-04-14T12:00:00Z"),
                Map.of(),
                new ArtifactContentPointer("workspace", "artifacts/report.md", "text/markdown", 42)
        );
        MemoryEntry memoryEntry = new MemoryEntry();
        memoryEntry.setType(MemoryType.FACT);

        assertThat(task.persistenceLifecycle()).isEqualTo(PersistenceLifecycle.DURABLE);
        assertThat(task.persistableFamily()).isEqualTo(PersistableObjectFamily.TASK);
        assertThat(message.persistableFamily()).isEqualTo(PersistableObjectFamily.MESSAGE);
        assertThat(traceEvent.persistableFamily()).isEqualTo(PersistableObjectFamily.TRACE);
        assertThat(artifact.persistableFamily()).isEqualTo(PersistableObjectFamily.ARTIFACT);
        assertThat(artifact.persistenceDomain()).isEqualTo("report");
        assertThat(memoryEntry.persistableFamily()).isEqualTo(PersistableObjectFamily.MEMORY);
        assertThat(memoryEntry.persistenceDomain()).isEqualTo("semantic_fact");
    }

    @Test
    void marksRuntimeOnlyObjectsAsEphemeral() {
        MemoryContext memoryContext = new MemoryContext("ctx", List.of(1L, 2L));
        TaskPrompt prompt = new TaskPrompt("sys", "usr");
        ConversationMemoryState state = new ConversationMemoryState("agent-1", "conv-1", List.of(), List.of(), Instant.now());

        assertThat(memoryContext.persistenceLifecycle()).isEqualTo(PersistenceLifecycle.EPHEMERAL);
        assertThat(prompt.persistenceLifecycle()).isEqualTo(PersistenceLifecycle.EPHEMERAL);
        assertThat(state.persistenceLifecycle()).isEqualTo(PersistenceLifecycle.EPHEMERAL);
    }
}
