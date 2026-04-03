package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmOrchestrationValidatorTest {

    private final LlmOrchestrationValidator validator = new LlmOrchestrationValidator();

    @Test
    void rejectsMissingProvider() {
        AgentDefinition definition = definition("", "model", "prompt");

        assertThatThrownBy(() -> validator.validate(definition))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("LLM provider");
    }

    @Test
    void acceptsValidDefinition() {
        AgentDefinition definition = definition("provider", "model", "prompt");
        assertThatCode(() -> validator.validate(definition)).doesNotThrowAnyException();
    }

    private AgentDefinition definition(String provider, String model, String prompt) {
        return new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.CHAT,
                provider,
                model,
                prompt,
                "default",
                true
        );
    }
}
