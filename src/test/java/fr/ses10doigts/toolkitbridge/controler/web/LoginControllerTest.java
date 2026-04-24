package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.security.admin.AdminAuthenticationService;
import fr.ses10doigts.toolkitbridge.security.admin.AdminLoginRateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    @Test
    void redirectsAuthenticatedUserAwayFromLoginPage() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        when(authenticationService.isAuthenticated(any())).thenReturn(true);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.login(request);

        assertThat(view).isEqualTo("redirect:/admin");
    }

    @Test
    void authenticatesAndRedirectsToAdminWhenTokenIsValid() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        when(authenticationService.authenticate(any(), eq("valid-token"))).thenReturn(true);
        when(rateLimiterService.isBlocked("127.0.0.1")).thenReturn(false);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("valid-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin");
        assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
        verify(rateLimiterService).reset("127.0.0.1");
    }

    @Test
    void redirectsBackToLoginWithErrorWhenTokenIsInvalid() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        when(authenticationService.authenticate(any(), eq("bad-token"))).thenReturn(false);
        when(rateLimiterService.isBlocked("127.0.0.1")).thenReturn(false);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("bad-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("error");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Invalid token.");
        verify(rateLimiterService).recordFailure("127.0.0.1");
    }

    @Test
    void logoutDelegatesToAuthenticationService() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.logout(request);

        assertThat(view).isEqualTo("redirect:/login");
        verify(authenticationService).logout(request);
    }

    @Test
    void rejectsBlockedIpBeforeAuthenticating() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        when(rateLimiterService.isBlocked("127.0.0.1")).thenReturn(true);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("bad-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Too many login attempts. Please retry in a few minutes.");
        verify(authenticationService, never()).authenticate(any(), any());
        verify(rateLimiterService, never()).recordFailure(any());
        verify(rateLimiterService, never()).reset(any());
    }

    @Test
    void successfulLoginResetsRateLimiterAfterPreviousFailures() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        AdminLoginRateLimiterService rateLimiterService = mock(AdminLoginRateLimiterService.class);
        when(rateLimiterService.isBlocked("127.0.0.1")).thenReturn(false);
        when(authenticationService.authenticate(any(), eq("valid-token"))).thenReturn(true);
        LoginController controller = new LoginController(authenticationService, rateLimiterService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("valid-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin");
        verify(rateLimiterService).reset("127.0.0.1");
        verify(rateLimiterService, never()).recordFailure(any());
    }
}
