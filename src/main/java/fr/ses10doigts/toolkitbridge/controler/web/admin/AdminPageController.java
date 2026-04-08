package fr.ses10doigts.toolkitbridge.controler.web.admin;

import fr.ses10doigts.toolkitbridge.model.dto.web.admin.AdminDashboardView;
import fr.ses10doigts.toolkitbridge.service.admin.web.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        AdminDashboardView dashboardView = adminDashboardService.buildView();
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("summaryCards", dashboardView.summaryCards());
        model.addAttribute("shortcutLinks", dashboardView.shortcutLinks());
        model.addAttribute("systemItems", dashboardView.systemItems());
        return "admin/dashboard";
    }

    @GetMapping("/ui-preview")
    public String uiPreview(Model model) {
        model.addAttribute("activeNav", "dashboard");
        return "admin/ui-preview";
    }

    private String renderPlaceholder(
            Model model,
            String activeNav,
            String pageTitle,
            String sectionTitle,
            String sectionDescription,
            List<String> todoItems
    ) {
        model.addAttribute("activeNav", activeNav);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("sectionTitle", sectionTitle);
        model.addAttribute("sectionDescription", sectionDescription);
        model.addAttribute("todoItems", todoItems);
        return "admin/placeholder";
    }
}
