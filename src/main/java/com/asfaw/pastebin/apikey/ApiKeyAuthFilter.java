package com.asfaw.pastebin.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    public static final String ATTR_KEY_ID = "pastebin.apiKey.id";
    public static final String ATTR_KEY_LIMIT = "pastebin.apiKey.limit";

    private final ApiKeyService apiKeyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented != null) {
            Optional<ApiKeyAuth> auth = apiKeyService.authenticate(presented);
            if (auth.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"detail\":\"Invalid API key\"}");
                return;
            }
            ApiKeyAuth keyAuth = auth.get();
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(
                            keyAuth.username(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            request.setAttribute(ATTR_KEY_ID, keyAuth.keyId());
            request.setAttribute(ATTR_KEY_LIMIT, keyAuth.rateLimitPerMinute());
        }
        chain.doFilter(request, response);
    }
}
