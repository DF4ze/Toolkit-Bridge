package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.security.admin.AdminAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    @Test
    void redirectsAuthenticatedUserAwayFromLoginPage() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.isAuthenticated(any())).thenReturn(true);
        LoginController controller = new LoginController(authenticationService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.login(request);

        assertThat(view).isEqualTo("redirect:/admin");
    }

    @Test
    void authenticatesAndRedirectsToAdminWhenTokenIsValid() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.authenticate(any(), eq("valid-token"))).thenReturn(true);
        LoginController controller = new LoginController(authenticationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("valid-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin");
        assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }

    @Test
    void redirectsBackToLoginWithErrorWhenTokenIsInvalid() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.authenticate(any(), eq("bad-token"))).thenReturn(false);
        LoginController controller = new LoginController(authenticationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.submitLogin("bad-token", request, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/login");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("error");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Invalid token.");
    }

    @Test
    void logoutDelegatesToAuthenticationService() {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        LoginController controller = new LoginController(authenticationService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.logout(request);

        assertThat(view).isEqualTo("redirect:/login");
        verify(authenticationService).logout(request);
    }
}
