package fr.ses10doigts.toolkitbridge.service.agent.communication.model;

import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactReference;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentMessageTest {

    @Test
    void createsMessageWithGeneratedIdsAndTimestamp() {
        AgentMessagePayload payload = new AgentMessagePayload(
                "Delegate this objective",
                "internal",
                "agent-1",
                "conv-1",
                null,
                Map.of("priority", "high"),
                List.of(new ArtifactReference("artifact-1", ArtifactType.PLAN))
        );

        AgentMessage message = AgentMessage.create(
                "agent-1",
                AgentRecipient.forRole(AgentRole.ASSISTANT),
                AgentMessageType.TASK_REQUEST,
                payload
        );

        assertThat(message.messageId()).isNotBlank();
        assertThat(message.correlationId()).isNotBlank();
        assertThat(message.timestamp()).isNotNull();
        assertThat(message.recipient().kind()).isEqualTo(AgentRecipientKind.ROLE);
        assertThat(message.payload().context()).containsEntry("priority", "high");
        assertThat(message.payload().artifacts()).hasSize(1);
    }

    @Test
    void rejectsBlankSender() {
        AgentMessagePayload payload = new AgentMessagePayload(
                "Question",
                "internal",
                null,
                null,
                null,
                Map.of()
        );

        assertThatThrownBy(() -> new AgentMessage(
                "m-1",
                "c-1",
                " ",
                AgentRecipient.forAgent("agent-2"),
                java.time.Instant.now(),
                AgentMessageType.QUESTION,
                payload
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("senderAgentId");
    }
}
