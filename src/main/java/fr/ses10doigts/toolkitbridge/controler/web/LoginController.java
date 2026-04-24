package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.security.admin.AdminAuthenticationService;
import fr.ses10doigts.toolkitbridge.security.admin.AdminLoginRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final AdminAuthenticationService adminAuthenticationService;
    private final AdminLoginRateLimiterService adminLoginRateLimiterService;

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (adminAuthenticationService.isAuthenticated(request)) {
            return "redirect:/admin";
        }
        return "login";
    }

    @PostMapping("/login")
    public String submitLogin(
            @RequestParam("token") String token,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        String clientIp = request.getRemoteAddr();
        if (adminLoginRateLimiterService.isBlocked(clientIp)) {
            redirectAttributes.addFlashAttribute("error", "Too many login attempts. Please retry in a few minutes.");
            return "redirect:/login";
        }

        boolean authenticated = adminAuthenticationService.authenticate(request, token);
        if (authenticated) {
            adminLoginRateLimiterService.reset(clientIp);
            return "redirect:/admin";
        }

        adminLoginRateLimiterService.recordFailure(clientIp);
        redirectAttributes.addFlashAttribute("error", "Invalid token.");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        adminAuthenticationService.logout(request);
        return "redirect:/login";
    }
}
