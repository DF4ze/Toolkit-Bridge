package fr.ses10doigts.toolkitbridge.controler.web.admin.llm;

import fr.ses10doigts.toolkitbridge.model.dto.admin.llm.LlmAdminResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.admin.LlmAdminForm;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminFacade;
import fr.ses10doigts.toolkitbridge.service.admin.functional.LlmAdminValidationException;
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
@RequestMapping("/admin/llms")
@RequiredArgsConstructor
public class LlmAdminPageController {

    private final LlmAdminFacade llmAdminFacade;

    @GetMapping
    public String list(Model model) {
        List<LlmAdminResponse> llms = llmAdminFacade.listLlms();
        model.addAttribute("activeNav", "llms");
        model.addAttribute("activeLlmNav", "list");
        model.addAttribute("llms", llms);
        return "admin/llms/list";
    }

    @GetMapping("/{llmId}")
    public String detail(@PathVariable String llmId, Model model) {
        return llmAdminFacade.getLlm(llmId)
                .map(llm -> {
                    model.addAttribute("activeNav", "llms");
                    model.addAttribute("activeLlmNav", "list");
                    model.addAttribute("llm", llm);
                    return "admin/llms/detail";
                })
                .orElse("redirect:/admin/llms");
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        LlmAdminForm form = new LlmAdminForm();
        prepareFormPage(
                model,
                form,
                false,
                "/admin/llms",
                "Marcel | Nouveau LLM",
                "Nouveau LLM"
        );
        return "admin/llms/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") LlmAdminForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepareFormPage(
                    model,
                    form,
                    false,
                    "/admin/llms",
                    "Marcel | Nouveau LLM",
                    "Nouveau LLM"
            );
            return "admin/llms/form";
        }

        try {
            LlmAdminResponse created = llmAdminFacade.createLlm(
                    form.getLlmId(),
                    form.getBaseUrl(),
                    form.getDefaultModel(),
                    form.getApiKey()
            );
            redirectAttributes.addFlashAttribute("successMessage", "LLM created successfully.");
            redirectAttributes.addFlashAttribute("infoMessage", "Configuration saved. Runtime provider registry may require restart to reload.");
            return "redirect:/admin/llms/" + created.llmId();
        } catch (LlmAdminValidationException exception) {
            bindingResult.reject("formError", exception.getMessage());
            prepareFormPage(
                    model,
                    form,
                    false,
                    "/admin/llms",
                    "Marcel | Nouveau LLM",
                    "Nouveau LLM"
            );
            return "admin/llms/form";
        }
    }

    @GetMapping("/{llmId}/edit")
    public String editForm(@PathVariable String llmId, Model model) {
        return llmAdminFacade.getLlm(llmId)
                .map(llm -> {
                    LlmAdminForm form = new LlmAdminForm();
                    form.setLlmId(llm.llmId());
                    form.setBaseUrl(llm.baseUrl());
                    form.setDefaultModel(llm.defaultModel());
                    form.setApiKey("");

                    prepareFormPage(
                            model,
                            form,
                            true,
                            "/admin/llms/" + llm.llmId(),
                            "Marcel | Edit LLM",
                            "Edit LLM"
                    );
                    model.addAttribute("apiKeyConfigured", llm.apiKeyConfigured());
                    return "admin/llms/form";
                })
                .orElse("redirect:/admin/llms");
    }

    @PostMapping("/{llmId}")
    public String update(
            @PathVariable String llmId,
            @Valid @ModelAttribute("form") LlmAdminForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        form.setLlmId(llmId);
        if (bindingResult.hasErrors()) {
            setApiKeyConfigured(model, llmId);
            prepareFormPage(
                    model,
                    form,
                    true,
                    "/admin/llms/" + llmId,
                    "Marcel | Edit LLM",
                    "Edit LLM"
            );
            return "admin/llms/form";
        }

        try {
            return llmAdminFacade.updateLlm(
                            llmId,
                            form.getBaseUrl(),
                            form.getDefaultModel(),
                            form.getApiKey()
                    )
                    .map(updated -> {
                        redirectAttributes.addFlashAttribute("successMessage", "LLM updated successfully.");
                        redirectAttributes.addFlashAttribute("infoMessage", "Configuration saved. Runtime provider registry may require restart to reload.");
                        return "redirect:/admin/llms/" + updated.llmId();
                    })
                    .orElse("redirect:/admin/llms");
        } catch (LlmAdminValidationException exception) {
            bindingResult.reject("formError", exception.getMessage());
            setApiKeyConfigured(model, llmId);
            prepareFormPage(
                    model,
                    form,
                    true,
                    "/admin/llms/" + llmId,
                    "Marcel | Edit LLM",
                    "Edit LLM"
            );
            return "admin/llms/form";
        }
    }

    private void prepareFormPage(
            Model model,
            LlmAdminForm form,
            boolean editMode,
            String cancelHref,
            String pageTitle,
            String formTitle
    ) {
        model.addAttribute("activeNav", "llms");
        model.addAttribute("activeLlmNav", editMode ? "edit" : "new");
        model.addAttribute("form", form);
        model.addAttribute("editMode", editMode);
        model.addAttribute("cancelHref", cancelHref);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formTitle", formTitle);
        if (!model.containsAttribute("apiKeyConfigured")) {
            model.addAttribute("apiKeyConfigured", false);
        }
    }

    private void setApiKeyConfigured(Model model, String llmId) {
        boolean apiKeyConfigured = llmAdminFacade.getLlm(llmId)
                .map(LlmAdminResponse::apiKeyConfigured)
                .orElse(false);
        model.addAttribute("apiKeyConfigured", apiKeyConfigured);
    }
}
