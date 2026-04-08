package fr.ses10doigts.toolkitbridge.security.admin;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminAuthFilterTest {

    @Test
    void redirectsUnauthenticatedWebAdminRequestToLogin() throws ServletException, IOException {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.isAuthenticated(any())).thenReturn(false);
        AdminAuthFilter filter = new AdminAuthFilter(authenticationService, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void returnsUnauthorizedForUnauthenticatedAdminApiRequest() throws ServletException, IOException {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.isAuthenticated(any())).thenReturn(false);
        AdminAuthFilter filter = new AdminAuthFilter(authenticationService, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/technical/overview");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("Admin authentication required");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void passesThroughWhenSessionIsAuthenticated() throws ServletException, IOException {
        AdminAuthenticationService authenticationService = mock(AdminAuthenticationService.class);
        when(authenticationService.isAuthenticated(any())).thenReturn(true);
        AdminAuthFilter filter = new AdminAuthFilter(authenticationService, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
