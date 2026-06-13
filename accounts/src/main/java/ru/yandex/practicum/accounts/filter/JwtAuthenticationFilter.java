package ru.yandex.practicum.accounts.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.yandex.practicum.accounts.service.UserDetailsServiceImpl;
import ru.yandex.practicum.accounts.util.JwtUtil;

import java.io.IOException;

/**
 * Фильтр для извлечения и валидации JWT токена Accounts сервиса
 * и загрузки UserDetails из базы данных для последующей авторизации.
 * Используется только для пользовательских JWT токенов (от Accounts сервиса).
 * JWT токены от Keycloak (Client Credentials) обрабатываются OAuth2 Resource Server.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    private final JwtDecoder keycloakJwtDecoder;

    public JwtAuthenticationFilter(
            UserDetailsServiceImpl userDetailsService,
            JwtUtil jwtUtil,
            JwtDecoder keycloakJwtDecoder) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.keycloakJwtDecoder = keycloakJwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("JwtAuthenticationFilter: Processing request {}", request.getRequestURI());

        String token = resolveToken(request);

        if (token != null) {
            try {
                // Проверяем, является ли токен Keycloak токеном
                boolean isKeycloakToken = isKeycloakToken(token);

                if (isKeycloakToken) {
                    // Keycloak токены обрабатываются OAuth2 Resource Server
                    log.debug("JwtAuthenticationFilter: Keycloak token detected, skipping");
                } else {
                    // Локальный токен Accounts сервиса - валидируем и загружаем пользователя
                    String login = jwtUtil.extractLogin(token);
                    log.debug("JwtAuthenticationFilter: Extracted login from JWT: {}", login);

                    if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(login);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.info("JwtAuthenticationFilter: Successfully authenticated user: {}", login);
                    }
                }
            } catch (JwtException e) {
                // Не является валидным JWT
                log.debug("JwtAuthenticationFilter: Not a valid JWT: {}", e.getMessage());
            } catch (Exception e) {
                log.warn("JwtAuthenticationFilter: Error processing JWT: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Проверяет, является ли токен JWT от Keycloak.
     * Keycloak токены имеют стандартные claims (iss, aud, realm_access).
     */
    private boolean isKeycloakToken(String token) {
        try {
            Jwt jwt = keycloakJwtDecoder.decode(token);
            // Keycloak токены содержат claim "realm_access" или "resource_access"
            return jwt.hasClaim("realm_access") || jwt.hasClaim("resource_access");
        } catch (JwtException e) {
            // Не является Keycloak токеном
            return false;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
