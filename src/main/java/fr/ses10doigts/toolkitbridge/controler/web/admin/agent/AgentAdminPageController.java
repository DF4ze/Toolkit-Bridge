package fr.ses10doigts.toolkitbridge.controler.web.admin.agent;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentAdminPageView;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AgentDefinitionAdminForm;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentAdminValidationException;
import fr.ses10doigts.toolkitbridge.service.admin.functional.AgentDefinitionAdminFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/agents")
@RequiredArgsConstructor
public class AgentAdminPageController {

    private final AgentDefinitionAdminFacade agentDefinitionAdminFacade;

    @GetMapping
    public String list(Model model) {
        List<AgentAdminPageView.AgentItem> agents = agentDefinitionAdminFacade.listAgents();
        applyNavigation(model, "list");
        model.addAttribute("agents", agents);
        return "admin/agents/list";
    }

    @GetMapping("/{agentId}")
    public String detail(@PathVariable String agentId, Model model) {
        return agentDefinitionAdminFacade.getAgent(agentId)
                .map(agent -> {
                    applyNavigation(model, "list");
                    model.addAttribute("agent", agent);
                    return "admin/agents/detail";
                })
                .orElse("redirect:/admin/agents");
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        AgentDefinitionAdminForm form = new AgentDefinitionAdminForm();
        form.setOrchestratorType(AgentOrchestratorType.CHAT.name());
        form.setRole(AgentRole.ASSISTANT.name());
        form.setPolicyName("default");
        form.setToolsEnabled(true);
        prepareForm(model, form, false, "/admin/agents", "Marcel | New Agent", "New Agent");
        return "admin/agents/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") AgentDefinitionAdminForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, false, "/admin/agents", "Marcel | New Agent", "New Agent");
            return "admin/agents/form";
        }

        try {
            AgentAdminPageView.AgentItem created = agentDefinitionAdminFacade.createAgentDefinition(form);
            redirectAttributes.addFlashAttribute("successMessage", "Agent definition created successfully.");
            return "redirect:/admin/agents/" + created.agentId();
        } catch (AgentAdminValidationException exception) {
            bindingResult.reject("formError", exception.getMessage());
            prepareForm(model, form, false, "/admin/agents", "Marcel | New Agent", "New Agent");
            return "admin/agents/form";
        }
    }

    @GetMapping("/{agentId}/edit")
    public String editForm(@PathVariable String agentId, Model model) {
        return agentDefinitionAdminFacade.getAgent(agentId)
                .map(agent -> {
                    AgentDefinitionAdminForm form = new AgentDefinitionAdminForm();
                    form.setAgentId(agent.agentId());
                    form.setName(agent.name());
                    form.setTelegramBotId(agent.telegramBotId());
                    form.setOrchestratorType(agent.orchestratorType());
                    form.setLlmProvider(agent.llmProvider());
                    form.setModel(agent.model());
                    form.setRole(agent.role());
                    form.setPolicyName(agent.policyName());
                    form.setToolsEnabled(agent.toolsEnabled());
                    form.setSystemPrompt(agent.systemPrompt());

                    prepareForm(model, form, true, "/admin/agents/" + agent.agentId(), "Marcel | Edit Agent", "Edit Agent");
                    return "admin/agents/form";
                })
                .orElse("redirect:/admin/agents");
    }

    @PostMapping("/{agentId}")
    public String update(
            @PathVariable String agentId,
            @Valid @ModelAttribute("form") AgentDefinitionAdminForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        form.setAgentId(agentId);
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, true, "/admin/agents/" + agentId, "Marcel | Edit Agent", "Edit Agent");
            return "admin/agents/form";
        }

        try {
            return agentDefinitionAdminFacade.updateAgentDefinition(agentId, form)
                    .map(updated -> {
                        redirectAttributes.addFlashAttribute("successMessage", "Agent definition updated successfully.");
                        return "redirect:/admin/agents/" + updated.agentId();
                    })
                    .orElse("redirect:/admin/agents");
        } catch (AgentAdminValidationException exception) {
            bindingResult.reject("formError", exception.getMessage());
            prepareForm(model, form, true, "/admin/agents/" + agentId, "Marcel | Edit Agent", "Edit Agent");
            return "admin/agents/form";
        }
    }

    private void prepareForm(
            Model model,
            AgentDefinitionAdminForm form,
            boolean editMode,
            String cancelHref,
            String pageTitle,
            String formTitle
    ) {
        applyNavigation(model, editMode ? "edit" : "new");
        model.addAttribute("form", form);
        model.addAttribute("editMode", editMode);
        model.addAttribute("cancelHref", cancelHref);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formTitle", formTitle);
        model.addAttribute("llmOptions", agentDefinitionAdminFacade.listLlmOptions());
        model.addAttribute("botOptions", agentDefinitionAdminFacade.listTelegramBotOptions());
        model.addAttribute("orchestratorOptions", AgentOrchestratorType.values());
        model.addAttribute("roleOptions", AgentRole.values());
    }

    private void applyNavigation(Model model, String activeAgentNav) {
        model.addAttribute("activeNav", "agents");
        model.addAttribute("activeAgentNav", activeAgentNav);
    }
}
