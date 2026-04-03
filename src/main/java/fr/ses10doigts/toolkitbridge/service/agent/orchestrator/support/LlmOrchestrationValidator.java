package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support;

import fr.ses10doigts.toolkitbridge.exception.AgentException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import org.springframework.stereotype.Component;

@Component
public class LlmOrchestrationValidator {

    public void validate(AgentDefinition definition) {
        if (definition == null) {
            throw new AgentException("runtime definition must not be null");
        }
        if (definition.llmProvider() == null || definition.llmProvider().isBlank()) {
            throw new AgentException("Agent LLM provider must not be blank");
        }
        if (definition.model() == null || definition.model().isBlank()) {
            throw new AgentException("Agent model must not be blank");
        }
        if (definition.systemPrompt() == null || definition.systemPrompt().isBlank()) {
            throw new AgentException("Agent system prompt must not be blank");
        }
    }
}
