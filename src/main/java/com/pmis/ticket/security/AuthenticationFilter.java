package com.pmis.ticket.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates the bearer token on every business endpoint via the user-service introspect API
 * before the request reaches a controller. Swagger/API-docs paths are exempt.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/swagger-ui", "/api-docs", "/v3/api-docs");

    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer "))
                ? header.substring(7).trim()
                : null;

        if (token == null || token.isBlank()) {
            reject(response, "Missing or malformed Authorization header");
            return;
        }

        AuthenticatedUser user = authClient.introspect(token);
        if (user == null) {
            reject(response, "Invalid, expired, or inactive access token");
            return;
        }

        try {
            AuthContext.set(user);
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", null);
        body.put("message", null);
        body.put("error", message);
        body.put("status", 401);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
