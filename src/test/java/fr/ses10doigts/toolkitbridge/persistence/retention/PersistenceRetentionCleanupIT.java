package fr.ses10doigts.toolkitbridge.persistence.retention;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshotEntity;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshotRepository;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionEntity;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionKind;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionRepository;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionStatus;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceEntity;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/persistence-retention-cleanup-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.persistence.retention.families.trace.domains." + RetentionDomains.TRACE_CRITICAL + ".ttl=7d",
                "toolkit.persistence.retention.families.trace.domains." + RetentionDomains.TRACE_CRITICAL + ".disposition=PURGE",
                "toolkit.persistence.retention.families.task.domains." + RetentionDomains.TASK_ADMIN_SNAPSHOT + ".ttl=30d",
                "toolkit.persistence.retention.families.task.domains." + RetentionDomains.TASK_ADMIN_SNAPSHOT + ".disposition=PURGE",
                "toolkit.persistence.retention.cleanup.enabled=false",
                "toolkit.llm.openai-like.providers[0].name=seed",
                "toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                "toolkit.llm.openai-like.providers[0].api-key=",
                "toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistenceRetentionCleanupIT {

    @Autowired
    private PersistenceRetentionCleanupService cleanupService;

    @Autowired
    private CriticalAgentTraceRepository traceRepository;

    @Autowired
    private AdminTaskSnapshotRepository taskRepository;

    @Autowired
    private HumanInterventionRepository humanInterventionRepository;

    @BeforeEach
    void setUp() {
        humanInterventionRepository.deleteAll();
        taskRepository.deleteAll();
        traceRepository.deleteAll();
    }

    @Test
    void cleanupRemovesExpiredTraceAndTaskButKeepsRecentAndHumanInterventions() {
        Instant now = Instant.now();

        traceRepository.save(trace("trace-old", now.minusSeconds(8 * 24 * 3600L)));
        traceRepository.save(trace("trace-recent", now.minusSeconds(2 * 24 * 3600L)));

        taskRepository.save(task("task-old", now.minusSeconds(45 * 24 * 3600L), now.minusSeconds(45 * 24 * 3600L)));
        taskRepository.save(task("task-recent", now.minusSeconds(40 * 24 * 3600L), now.minusSeconds(5 * 24 * 3600L)));

        humanInterventionRepository.save(humanIntervention("req-old", now.minusSeconds(90 * 24 * 3600L)));
        humanInterventionRepository.save(humanIntervention("req-recent", now.minusSeconds(2 * 24 * 3600L)));

        cleanupService.cleanup();

        assertThat(traceRepository.findAll()).hasSize(1);
        assertThat(traceRepository.findAll())
                .extracting(CriticalAgentTraceEntity::getRunId)
                .containsExactly("trace-recent");

        assertThat(taskRepository.findAll()).hasSize(1);
        assertThat(taskRepository.findAll())
                .extracting(AdminTaskSnapshotEntity::getTaskId)
                .containsExactly("task-recent");

        assertThat(humanInterventionRepository.findAll()).hasSize(2);
        assertThat(humanInterventionRepository.findAll())
                .extracting(HumanInterventionEntity::getRequestId)
                .containsExactlyInAnyOrder("req-old", "req-recent");
    }

    private CriticalAgentTraceEntity trace(String runId, Instant occurredAt) {
        CriticalAgentTraceEntity entity = new CriticalAgentTraceEntity();
        entity.setOccurredAt(occurredAt);
        entity.setEventType(AgentTraceEventType.ERROR);
        entity.setSource("it");
        entity.setRunId(runId);
        entity.setAgentId("agent-a");
        entity.setMessageId("msg-" + runId);
        entity.setTaskId("task-" + runId);
        entity.setAttributesJson("{}");
        entity.setIngestedAt(occurredAt);
        return entity;
    }

    private AdminTaskSnapshotEntity task(String taskId, Instant firstSeenAt, Instant lastSeenAt) {
        AdminTaskSnapshotEntity entity = new AdminTaskSnapshotEntity();
        entity.setTaskId(taskId);
        entity.setParentTaskId(null);
        entity.setObjective("objective-" + taskId);
        entity.setInitiator("user-1");
        entity.setAssignedAgentId("agent-a");
        entity.setTraceId("trace-" + taskId);
        entity.setEntryPoint(TaskEntryPoint.TASK_ORCHESTRATOR);
        entity.setStatus(TaskStatus.DONE);
        entity.setChannelType("telegram");
        entity.setConversationId("conv-" + taskId);
        entity.setFirstSeenAt(firstSeenAt);
        entity.setLastSeenAt(lastSeenAt);
        entity.setErrorMessage(null);
        entity.setArtifactCount(0);
        return entity;
    }

    private HumanInterventionEntity humanIntervention(String requestId, Instant createdAt) {
        HumanInterventionEntity entity = new HumanInterventionEntity();
        entity.setRequestId(requestId);
        entity.setCreatedAt(createdAt);
        entity.setTraceId("trace-" + requestId);
        entity.setAgentId("agent-a");
        entity.setSensitiveAction(AgentSensitiveAction.TOOL_EXECUTION);
        entity.setKind(HumanInterventionKind.APPROVAL);
        entity.setStatus(HumanInterventionStatus.PENDING);
        entity.setSummary("summary");
        entity.setDetail("detail");
        entity.setRequestMetadataJson("{}");
        return entity;
    }
}
