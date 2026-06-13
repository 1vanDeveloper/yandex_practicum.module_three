package ru.yandex.practicum.accounts.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.yandex.practicum.accounts.service.UserDetailsServiceImpl;

import java.io.IOException;

/**
 * Фильтр для извлечения логина пользователя из JWT токена Keycloak
 * и загрузки UserDetails из базы данных для последующей авторизации.
 *
 * OAuth2 Resource Server уже валидирует JWT токен, этот фильтр только
 * загружает информацию о пользователе из БД для доступа к аккаунту.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("JwtAuthenticationFilter: Processing request {}", request.getRequestURI());

        // Проверяем, есть ли уже аутентификация от OAuth2 Resource Server
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String login = jwt.getClaimAsString("preferred_username");

            log.debug("JwtAuthenticationFilter: Extracted login from Keycloak JWT: {}", login);

            if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(login);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("JwtAuthenticationFilter: Successfully loaded user details for: {}", login);
                } catch (Exception e) {
                    log.warn("JwtAuthenticationFilter: User not found in local database: {}", login);
                    // Продолжаем с JWT аутентификацией, даже если пользователь не найден в локальной БД
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
