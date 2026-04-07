package fr.ses10doigts.toolkitbridge.service.agent.debate.service;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentMessage;
import fr.ses10doigts.toolkitbridge.service.agent.communication.model.AgentRecipient;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateContext;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateMessageCommand;
import fr.ses10doigts.toolkitbridge.service.agent.debate.model.DebateStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DebateMessageFactoryTest {

    private final DebateMessageFactory factory = new DebateMessageFactory();

    @Test
    void createsStructuredDebateQuestionAndKeepsCorrelationOnDebateId() {
        DebateContext debateContext = new DebateContext(
                "debate-42",
                "task-7",
                DebateStage.QUESTION,
                null,
                null,
                "artifact-9",
                "agent-alpha"
        );

        AgentMessage message = factory.create(new DebateMessageCommand(
                "agent-alpha",
                AgentRecipient.forRole(AgentRole.CRITIC),
                debateContext,
                "Please review this artifact",
                "internal",
                "agent-alpha",
                "conv-1",
                "project-1",
                Map.of("customKey", "customValue"),
                List.of(new ArtifactReference("artifact-9", ArtifactType.PLAN))
        ));

        assertThat(message.correlationId()).isEqualTo("debate-42");
        assertThat(message.type().name()).isEqualTo("QUESTION");
        assertThat(message.payload().context())
                .containsEntry("debateId", "debate-42")
                .containsEntry("debateStage", "QUESTION")
                .containsEntry("debateTaskId", "task-7")
                .containsEntry("debateSubjectArtifactId", "artifact-9")
                .containsEntry("debateInitiatorAgentId", "agent-alpha")
                .containsEntry("debateRootMessageId", message.messageId())
                .containsEntry("customKey", "customValue");
        assertThat(message.payload().artifacts()).hasSize(1);
    }

    @Test
    void extractsDebateContextFromStructuredMessage() {
        DebateContext debateContext = new DebateContext(
                "debate-42",
                "task-7",
                DebateStage.CRITIQUE,
                "root-1",
                "parent-1",
                "artifact-9",
                "agent-alpha"
        );

        AgentMessage message = factory.create(new DebateMessageCommand(
                "agent-beta",
                AgentRecipient.forRole(AgentRole.PLANNER),
                debateContext,
                "The plan misses rollback details",
                "internal",
                null,
                "conv-1",
                "project-1",
                Map.of(),
                List.of()
        ));

        assertThat(factory.extract(message))
                .hasValueSatisfying(extracted -> {
                    assertThat(extracted.debateId()).isEqualTo("debate-42");
                    assertThat(extracted.stage()).isEqualTo(DebateStage.CRITIQUE);
                    assertThat(extracted.rootMessageId()).isEqualTo("root-1");
                    assertThat(extracted.parentMessageId()).isEqualTo("parent-1");
                });
    }
}
