package ru.yandex.practicum.mybankfront.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.yandex.practicum.mybankfront.dto.JwtTokenResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Кастомный фильтр для обработки login формы через GatewayService.
 * Получает JWT токен от Accounts сервиса и сохраняет в сессии.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String SECURITY_CONTEXT_SESSION_ATTR = "SPRING_SECURITY_CONTEXT";
    private static final String JWT_SECRET = "mySecretKeyForJWTTokenGenerationMustBeLongEnough";

    private final GatewayService gatewayService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    public int getOrder() {
        return -101;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Проверяем, что это POST запрос на /api/auth/login
        if (!LOGIN_URL.equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Получаем логин и пароль из формы
        String login = request.getParameter("login");
        String password = request.getParameter("password");

        log.debug("CustomAuthenticationFilter: processing login for user: {}", login);

        try {
            // Вызываем GatewayService для получения JWT токена
            LoginRequest loginRequest = new LoginRequest(login, password);
            JwtTokenResponse tokenResponse = gatewayService.login(loginRequest).join();

            log.debug("CustomAuthenticationFilter: user {} authenticated successfully", login);

            // Извлекаем привилегии из JWT токена
            List<SimpleGrantedAuthority> authorities = extractPrivilegesFromToken(tokenResponse.getToken());

            // Создаём Authentication объект с токеном и привилегиями
            Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                login,
                tokenResponse.getToken(),
                authorities
            );

            // Сохраняем в SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Сохраняем SecurityContext в сессии
            HttpSession session = request.getSession(true);
            session.setAttribute(SECURITY_CONTEXT_SESSION_ATTR, securityContext);

            // Редирект на /account
            response.sendRedirect("/account");

        } catch (Exception ex) {
            log.error("CustomAuthenticationFilter: login failed for user {}: {}", login, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            // Редирект на /login?error
            response.sendRedirect("/login?error");
        }
    }

    /**
     * Извлекает привилегии из JWT токена.
     */
    private List<SimpleGrantedAuthority> extractPrivilegesFromToken(String token) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        try {
            byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Извлекаем привилегии из claim "privileges"
            List<String> privileges = claims.get("privileges", List.class);
            if (privileges != null) {
                for (String privilege : privileges) {
                    authorities.add(new SimpleGrantedAuthority(privilege));
                }
                log.debug("Extracted privileges from JWT: {}", privileges);
            }
        } catch (Exception e) {
            log.warn("Failed to extract privileges from JWT token: {}", e.getMessage());
        }
        
        return authorities;
    }
}
