package ru.yandex.practicum.mybankfront.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import ru.yandex.practicum.mybankfront.dto.JwtTokenResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Контроллер для страниц авторизации и главной страницы.
 * Аутентификация через форму с получением JWT от Accounts сервиса.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final GatewayService gatewayService;

    /**
     * GET /login - страница входа с формой.
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        log.debug("Login page requested");
        return "login";
    }

    /**
     * POST /api/auth/login - обработка формы входа.
     */
    @PostMapping("/api/auth/login")
    public RedirectView login(
            @RequestParam String login,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {

        log.info("AuthController: login form submitted for user: {}", login);

        LoginRequest loginRequest = new LoginRequest(login, password);

        try {
            JwtTokenResponse tokenResponse = gatewayService.login(loginRequest).join();
            log.info("AuthController: user {} authenticated successfully, token length: {}", login, tokenResponse.getToken() != null ? tokenResponse.getToken().length() : "null");
            
            // Извлекаем привилегии из JWT токена
            List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey("mySecretKeyForJWTTokenGenerationMustBeLongEnough".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(tokenResponse.getToken())
                    .getBody();
                List<String> privileges = claims.get("privileges", List.class);
                if (privileges != null) {
                    for (String privilege : privileges) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(privilege));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to extract privileges from JWT: {}", e.getMessage());
            }
            
            // Создаём Authentication объект
            org.springframework.security.core.Authentication authentication = 
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    login,
                    tokenResponse.getToken(),
                    authorities
                );
            
            // Сохраняем в SecurityContext
            org.springframework.security.core.context.SecurityContext securityContext = 
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
            
            // Сохраняем SecurityContext и JWT токен в сессии
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
            session.setAttribute("JWT_TOKEN", tokenResponse.getToken());
            log.info("AuthController: JWT token saved to session for user {}, session id: {}, token length: {}", login, session.getId(), tokenResponse.getToken().length());

            // Принудительно сохраняем сессию
            session.setMaxInactiveInterval(1800); // 30 минут
            log.info("AuthController: session created, max inactive interval: {}", session.getMaxInactiveInterval());

            return new RedirectView("/account");
        } catch (Exception ex) {
            log.error("AuthController: Login failed for user {}: {}", login, ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), ex);
            return new RedirectView("/login?error");
        }
    }

    /**
     * GET /register - страница регистрации.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.debug("Register page requested");
        return "register";
    }

    /**
     * POST /api/auth/register - обработка формы регистрации.
     */
    @PostMapping("/api/auth/register")
    public RedirectView register(
            @RequestParam String login,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String birthDate,
            Model model) {

        log.debug("Register form submitted for user: {}", login);

        RegisterRequest request = RegisterRequest.builder()
                .login(login)
                .password(password)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .birthDate(LocalDate.parse(birthDate))
                .amount(BigDecimal.ZERO)
                .build();

        try {
            gatewayService.register(request).join();
            log.debug("User {} registered successfully", login);
            return new RedirectView("/login?registered");
        } catch (Exception ex) {
            log.error("Registration failed: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            return new RedirectView("/register?error");
        }
    }

    /**
     * GET / - главная страница с редиректом на аккаунт или логин.
     */
    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
            return "redirect:/account";
        }
        return "redirect:/login";
    }
}
