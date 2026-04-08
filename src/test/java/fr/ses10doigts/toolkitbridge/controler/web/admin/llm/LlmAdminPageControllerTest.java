package fr.ses10doigts.toolkitbridge.controler.web.admin.llm;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class LlmAdminPageControllerTest {

    private LlmAdminFacade llmAdminFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        llmAdminFacade = mock(LlmAdminFacade.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LlmAdminPageController(llmAdminFacade)).build();
    }

    @Test
    void servesListAndDetailPages() throws Exception {
        LlmAdminResponse llm = new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true);
        when(llmAdminFacade.listLlms()).thenReturn(List.of(llm));
        when(llmAdminFacade.getLlm("openai")).thenReturn(Optional.of(llm));

        mockMvc.perform(get("/admin/llms"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/llms/list"))
                .andExpect(model().attribute("activeNav", "llms"))
                .andExpect(model().attribute("llms", hasSize(1)));

        mockMvc.perform(get("/admin/llms/openai"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/llms/detail"))
                .andExpect(model().attribute("llm", llm));

        verify(llmAdminFacade).listLlms();
        verify(llmAdminFacade).getLlm("openai");
    }

    @Test
    void servesCreateAndEditForms() throws Exception {
        LlmAdminResponse llm = new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true);
        when(llmAdminFacade.getLlm("openai")).thenReturn(Optional.of(llm));

        mockMvc.perform(get("/admin/llms/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/llms/form"))
                .andExpect(model().attribute("editMode", false));

        mockMvc.perform(get("/admin/llms/openai/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/llms/form"))
                .andExpect(model().attribute("editMode", true));
    }

    @Test
    void handlesCreateAndUpdateSubmissions() throws Exception {
        LlmAdminResponse created = new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5", true);
        LlmAdminResponse updated = new LlmAdminResponse("openai", "https://api.openai.com/v1", "gpt-5.1", true);

        when(llmAdminFacade.createLlm("openai", "https://api.openai.com/v1", "gpt-5", "secret"))
                .thenReturn(created);
        when(llmAdminFacade.updateLlm("openai", "https://api.openai.com/v1", "gpt-5.1", ""))
                .thenReturn(Optional.of(updated));

        mockMvc.perform(post("/admin/llms")
                        .param("llmId", "openai")
                        .param("baseUrl", "https://api.openai.com/v1")
                        .param("defaultModel", "gpt-5")
                        .param("apiKey", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/llms/openai"))
                .andExpect(flash().attributeExists("successMessage"));

        mockMvc.perform(post("/admin/llms/openai")
                        .param("llmId", "openai")
                        .param("baseUrl", "https://api.openai.com/v1")
                        .param("defaultModel", "gpt-5.1")
                        .param("apiKey", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/llms/openai"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void redisplaysFormOnValidationError() throws Exception {
        mockMvc.perform(post("/admin/llms")
                        .param("llmId", "")
                        .param("baseUrl", "")
                        .param("defaultModel", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/llms/form"))
                .andExpect(model().attributeHasFieldErrors("form", "llmId", "baseUrl", "defaultModel"));
    }
}
