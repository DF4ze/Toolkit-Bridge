package fr.ses10doigts.toolkitbridge.controler.web.admin.agent;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentAdminPageView;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentDefinitionAdminFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AgentAdminPageControllerTest {

    private AgentDefinitionAdminFacade facade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        facade = mock(AgentDefinitionAdminFacade.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentAdminPageController(facade)).build();

        when(facade.listLlmOptions()).thenReturn(List.of(new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true)));
        when(facade.listTelegramBotOptions()).thenReturn(List.of(new TelegramBotAdminResponse("bot-main", true, 123L, true, true)));
    }

    @Test
    void servesListAndDetailPages() throws Exception {
        AgentAdminPageView.AgentItem item = new AgentAdminPageView.AgentItem(
                "assistant-main", "Assistant", "CHAT", "ASSISTANT", "openai", "gpt-5", "bot-main", "default", true,
                "system prompt", true, Instant.parse("2026-01-01T00:00:00Z")
        );
        when(facade.listAgents()).thenReturn(List.of(item));
        when(facade.getAgent("assistant-main")).thenReturn(Optional.of(item));
        when(facade.getAgent("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/agents"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agents/list"))
                .andExpect(model().attribute("activeNav", "agents"))
                .andExpect(model().attribute("agents", hasSize(1)));

        mockMvc.perform(get("/admin/agents/assistant-main"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agents/detail"))
                .andExpect(model().attribute("agent", item));

        mockMvc.perform(get("/admin/agents/missing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/agents"));

        verify(facade).listAgents();
        verify(facade).getAgent("assistant-main");
        verify(facade).getAgent("missing");
    }

    @Test
    void handlesCreateAndEditFlow() throws Exception {
        AgentAdminPageView.AgentItem item = new AgentAdminPageView.AgentItem(
                "assistant-main", "Assistant", "CHAT", "ASSISTANT", "openai", "gpt-5", "bot-main", "default", true,
                "system prompt", true, Instant.parse("2026-01-01T00:00:00Z")
        );
        when(facade.getAgent("assistant-main")).thenReturn(Optional.of(item));
        when(facade.createAgentDefinition(org.mockito.ArgumentMatchers.any())).thenReturn(item);
        when(facade.updateAgentDefinition(org.mockito.ArgumentMatchers.eq("assistant-main"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(item));

        mockMvc.perform(get("/admin/agents/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agents/form"))
                .andExpect(model().attribute("editMode", false));

        mockMvc.perform(get("/admin/agents/assistant-main/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/agents/form"))
                .andExpect(model().attribute("editMode", true));

        mockMvc.perform(post("/admin/agents")
                        .param("agentId", "assistant-main")
                        .param("name", "Assistant")
                        .param("telegramBotId", "bot-main")
                        .param("orchestratorType", "CHAT")
                        .param("llmProvider", "openai")
                        .param("model", "gpt-5")
                        .param("systemPrompt", "System prompt")
                        .param("role", "ASSISTANT")
                        .param("policyName", "default")
                        .param("toolsEnabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/agents/assistant-main"));

        mockMvc.perform(post("/admin/agents/assistant-main")
                        .param("agentId", "assistant-main")
                        .param("name", "Assistant")
                        .param("telegramBotId", "bot-main")
                        .param("orchestratorType", "CHAT")
                        .param("llmProvider", "openai")
                        .param("model", "gpt-5")
                        .param("systemPrompt", "System prompt")
                        .param("role", "ASSISTANT")
                        .param("policyName", "default")
                        .param("toolsEnabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/agents/assistant-main"));
    }
}
