package ru.yandex.practicum.mybankfront.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для восстановления SecurityContext из сессии.
 * Проверяет наличие аутентификации в сессии для каждого запроса.
 */
@Component
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final String SECURITY_CONTEXT_SESSION_ATTR = "SPRING_SECURITY_CONTEXT";

    @Override
    public int getOrder() {
        return -102;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object securityContextObj = session.getAttribute(SECURITY_CONTEXT_SESSION_ATTR);
            if (securityContextObj instanceof SecurityContext securityContext) {
                Authentication authentication = securityContext.getAuthentication();
                if (authentication != null) {
                    log.debug("SessionAuthenticationFilter: restored authentication for user: {}", authentication.getName());
                    SecurityContextHolder.setContext(securityContext);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Очищаем SecurityContext после обработки запроса
            SecurityContextHolder.clearContext();
        }
    }
}
