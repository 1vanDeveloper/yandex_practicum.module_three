package ru.yandex.practicum.mybankfront.controller;

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
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.util.concurrent.CompletableFuture;

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
            Model model) {

        log.debug("Login form submitted for user: {}", login);

        LoginRequest request = new LoginRequest(login, password);

        try {
            JwtTokenResponse tokenResponse = gatewayService.login(request).join();
            // Сохраняем токен в сессии
            log.debug("User {} authenticated successfully", login);
            return new RedirectView("/account");
        } catch (Exception ex) {
            log.error("Login failed: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            return new RedirectView("/login?error");
        }
    }

    /**
     * GET /register - страница регистрации (информационная).
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        log.debug("Register page requested");
        return "register";
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
