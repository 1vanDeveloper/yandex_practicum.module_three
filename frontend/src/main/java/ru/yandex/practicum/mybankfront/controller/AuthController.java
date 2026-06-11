package ru.yandex.practicum.mybankfront.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;
import ru.yandex.practicum.mybankfront.service.GatewayService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для страниц авторизации и регистрации.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final GatewayService gatewayService;

    /**
     * GET /login - страница входа.
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * GET /register - страница регистрации.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    /**
     * POST /register - обработка формы регистрации.
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            List<String> errors = new ArrayList<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.add(error.getField() + ": " + error.getDefaultMessage()));
            model.addAttribute("errors", errors);
            return "register";
        }

        try {
            CompletableFuture<Void> future = gatewayService.register(request);
            future.join();
            
            model.addAttribute("success", "Регистрация успешна! Теперь вы можете войти.");
            return "login";
        } catch (Exception e) {
            log.error("Error during registration for login: {}", request.getLogin(), e);
            List<String> errors = new ArrayList<>();
            errors.add("Ошибка регистрации: " + e.getCause().getMessage());
            model.addAttribute("errors", errors);
            model.addAttribute("registerRequest", request);
            return "register";
        }
    }

    /**
     * GET / - главная страница с редиректом на аккаунт или логин.
     */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            return "redirect:/account";
        }
        return "redirect:/login";
    }
}
