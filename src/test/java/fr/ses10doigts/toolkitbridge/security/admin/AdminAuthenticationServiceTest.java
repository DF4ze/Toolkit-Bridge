package fr.ses10doigts.toolkitbridge.security.admin;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminAuthenticationServiceTest {

    @Test
    void createsAuthenticatedSessionWhenTokenMatches() {
        AdminTokenService tokenService = mock(AdminTokenService.class);
        when(tokenService.matches("good-token")).thenReturn(true);
        AdminAuthenticationService authenticationService = new AdminAuthenticationService(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean authenticated = authenticationService.authenticate(request, "good-token");

        assertThat(authenticated).isTrue();
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(AdminAuthenticationService.ADMIN_AUTHENTICATED_SESSION_KEY))
                .isEqualTo(Boolean.TRUE);
        assertThat(request.getSession(false).getAttribute(AdminAuthenticationService.ADMIN_AUTHENTICATED_AT_SESSION_KEY))
                .isNotNull();
    }

    @Test
    void rejectsInvalidTokenWithoutSessionCreation() {
        AdminTokenService tokenService = mock(AdminTokenService.class);
        when(tokenService.matches("bad-token")).thenReturn(false);
        AdminAuthenticationService authenticationService = new AdminAuthenticationService(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean authenticated = authenticationService.authenticate(request, "bad-token");

        assertThat(authenticated).isFalse();
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void invalidatesSessionOnLogout() {
        AdminTokenService tokenService = mock(AdminTokenService.class);
        AdminAuthenticationService authenticationService = new AdminAuthenticationService(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AdminAuthenticationService.ADMIN_AUTHENTICATED_SESSION_KEY, Boolean.TRUE);

        authenticationService.logout(request);

        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void isAuthenticatedChecksSessionFlag() {
        AdminTokenService tokenService = mock(AdminTokenService.class);
        AdminAuthenticationService authenticationService = new AdminAuthenticationService(tokenService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AdminAuthenticationService.ADMIN_AUTHENTICATED_SESSION_KEY, Boolean.TRUE);

        assertThat(authenticationService.isAuthenticated(request)).isTrue();

        request.getSession(false).setAttribute(AdminAuthenticationService.ADMIN_AUTHENTICATED_SESSION_KEY, Boolean.FALSE);
        assertThat(authenticationService.isAuthenticated(request)).isFalse();
    }
}
