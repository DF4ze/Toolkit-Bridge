package fr.ses10doigts.toolkitbridge.security.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminAuthenticationService {

    public static final String ADMIN_AUTHENTICATED_SESSION_KEY = "toolkit.admin.authenticated";
    public static final String ADMIN_AUTHENTICATED_AT_SESSION_KEY = "toolkit.admin.authenticatedAt";

    private final AdminTokenService adminTokenService;

    public AdminAuthenticationService(AdminTokenService adminTokenService) {
        this.adminTokenService = adminTokenService;
    }

    public boolean authenticate(HttpServletRequest request, String token) {
        if (!adminTokenService.matches(token)) {
            return false;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(ADMIN_AUTHENTICATED_SESSION_KEY, Boolean.TRUE);
        session.setAttribute(ADMIN_AUTHENTICATED_AT_SESSION_KEY, Instant.now().toString());
        return true;
    }

    public boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        return Boolean.TRUE.equals(session.getAttribute(ADMIN_AUTHENTICATED_SESSION_KEY));
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
