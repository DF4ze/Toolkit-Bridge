package fr.ses10doigts.toolkitbridge.service.agent.orchestrator.impl;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.LlmOrchestrationValidator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.MemoryRequestFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationRequestContextFactory;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.support.OrchestrationResponseSanitizer;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.llm.LlmService;
import fr.ses10doigts.toolkitbridge.service.llm.debug.LlmDebugStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAgentOrchestratorMemoryIntegrationTest {

    @Test
    void writesThenReusesMemoryAcrossTurns() {
        LlmService llmService = mock(LlmService.class);
        InMemoryMemoryFacade memoryFacade = new InMemoryMemoryFacade();
        LlmDebugStore llmDebugStore = mock(LlmDebugStore.class);

        ChatAgentOrchestrator orchestrator = new ChatAgentOrchestrator(
                llmService,
                llmDebugStore,
                new LlmOrchestrationValidator(),
                new OrchestrationRequestContextFactory(),
                new MemoryRequestFactory(),
                new OrchestrationResponseSanitizer()
        );

        AgentDefinition definition = new AgentDefinition(
                "agent-1",
                "Agent",
                "bot-1",
                AgentRole.ASSISTANT,
                AgentOrchestratorType.CHAT,
                "provider",
                "model",
                "system",
                "default",
                true
        );

        when(llmService.chat(eq("provider"), eq("model"), eq("system"), any(), eq(true)))
                .thenReturn("ok");

        AgentRuntime runtime = runtime(orchestrator, definition, memoryFacade);
        AgentResponse first = orchestrator.handle(runtime, request("first message"));
        AgentResponse second = orchestrator.handle(runtime, request("second message"));

        assertThat(first.error()).isFalse();
        assertThat(second.error()).isFalse();
        assertThat(memoryFacade.lastBuiltContext).contains("first message");
        assertThat(memoryFacade.lastBuiltContext).contains("second message");
    }

    private AgentRequest request(String message) {
        return new AgentRequest("agent-1", "telegram", "user-1", "chat-1", null, message, Map.of());
    }

    private AgentRuntime runtime(ChatAgentOrchestrator orchestrator, AgentDefinition definition, MemoryFacade memoryFacade) {
        AgentPolicy policy = new AgentPolicy() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public boolean allowTools(AgentRuntime runtime, AgentRequest request) {
                return true;
            }
        };
        return new AgentRuntime(
                definition,
                orchestrator,
                memoryFacade,
                new AgentToolAccess(true, Set.of("run_command")),
                policy,
                new AgentWorkspaceScope(null, null),
                new AgentRuntimeState()
        );
    }

    private static class InMemoryMemoryFacade implements MemoryFacade {
        private final List<String> history = new ArrayList<>();
        private String lastBuiltContext = "";

        @Override
        public MemoryContext buildContext(MemoryContextRequest request) {
            StringBuilder builder = new StringBuilder("history:\n");
            for (String item : history) {
                builder.append(item).append("\n");
            }
            builder.append("current:").append(request.currentUserMessage());
            lastBuiltContext = builder.toString();
            return new MemoryContext(lastBuiltContext, List.of());
        }

        @Override
        public void onUserMessage(MemoryContextRequest request) {
            history.add(request.currentUserMessage());
        }

        @Override
        public void onAssistantMessage(MemoryContextRequest request, String assistantMessage) {
            history.add(assistantMessage);
        }

        @Override
        public void onToolExecution(MemoryContextRequest request, ToolExecutionRecord toolExecution) {
        }

        @Override
        public void markContextMemoriesUsed(List<Long> semanticMemoryIds) {
        }
    }
}
