package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/human-intervention-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.llm.openai-like.providers[0].name=seed",
                "toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                "toolkit.llm.openai-like.providers[0].api-key=",
                "toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PersistentHumanInterventionServiceIT {

    @Autowired
    private HumanInterventionService service;

    @Autowired
    private HumanInterventionRepository repository;

    @Test
    void openAndFindPendingAndFindByIdShouldUsePersistentStorage() {
        HumanInterventionRequest first = service.open(new HumanInterventionDraft(
                "trace-" + UUID.randomUUID(),
                "agent-1",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                "Review deploy",
                null,
                java.util.Map.of("toolName", "deploy")
        ));
        HumanInterventionRequest second = service.open(new HumanInterventionDraft(
                "trace-" + UUID.randomUUID(),
                "agent-2",
                AgentSensitiveAction.DELEGATION,
                HumanInterventionKind.REVIEW,
                "Review delegation",
                "check recipient",
                java.util.Map.of("scope", "project")
        ));

        List<HumanInterventionRequest> pending = service.findPending();
        Optional<HumanInterventionRequest> reloaded = service.findById(first.requestId());

        assertThat(pending).extracting(HumanInterventionRequest::requestId).contains(first.requestId(), second.requestId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.orElseThrow().status()).isEqualTo(HumanInterventionStatus.PENDING);
        assertThat(repository.findByRequestId(first.requestId())).isPresent();
    }

    @Test
    void recordDecisionShouldUpdateOnlyPendingRows() {
        HumanInterventionRequest opened = service.open(new HumanInterventionDraft(
                "trace-" + UUID.randomUUID(),
                "agent-1",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                "Review dangerous command",
                null,
                java.util.Map.of()
        ));

        HumanInterventionDecision approve = new HumanInterventionDecision(
                opened.requestId(),
                HumanInterventionStatus.APPROVED,
                Instant.now(),
                "operator-1",
                "telegram",
                "approved",
                java.util.Map.of("reason", "safe")
        );

        Optional<HumanInterventionRequest> first = service.recordDecision(approve);
        Optional<HumanInterventionRequest> second = service.recordDecision(new HumanInterventionDecision(
                opened.requestId(),
                HumanInterventionStatus.REJECTED,
                Instant.now(),
                "operator-2",
                "telegram",
                "should be ignored",
                java.util.Map.of("reason", "late")
        ));

        assertThat(first).isPresent();
        assertThat(first.orElseThrow().status()).isEqualTo(HumanInterventionStatus.APPROVED);
        assertThat(second).isEmpty();
        HumanInterventionEntity persisted = repository.findByRequestId(opened.requestId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(HumanInterventionStatus.APPROVED);
        assertThat(persisted.getDecisionStatus()).isEqualTo(HumanInterventionStatus.APPROVED);
    }
}
