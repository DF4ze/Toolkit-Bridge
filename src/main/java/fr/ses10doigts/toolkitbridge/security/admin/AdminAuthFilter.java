package fr.ses10doigts.toolkitbridge.security.admin;

import fr.ses10doigts.toolkitbridge.model.dto.web.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminAuthFilter extends OncePerRequestFilter {

    private final AdminAuthenticationService adminAuthenticationService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = extractPath(request);
        return !isProtectedPath(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (adminAuthenticationService.isAuthenticated(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = extractPath(request);
        if (isApiPath(path)) {
            writeUnauthorizedResponse(response, request);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login");
    }

    private boolean isProtectedPath(String path) {
        return path.equals("/admin")
                || path.startsWith("/admin/")
                || path.equals("/api/admin")
                || path.startsWith("/api/admin/");
    }

    private boolean isApiPath(String path) {
        return path.equals("/api") || path.startsWith("/api/");
    }

    private String extractPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Admin authentication required",
                request.getRequestURI(),
                null
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
