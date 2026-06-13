package ru.yandex.practicum.mybankfront.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Контроллер для страниц авторизации и главной страницы.
 * OAuth2 Login обрабатывается автоматически Spring Security.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /**
     * GET /login - страница входа с кнопкой OAuth2 авторизации.
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        log.debug("Login page requested");
        return "login";
    }

    /**
     * GET /register - страница регистрации (информационная).
     * Регистрация пользователей осуществляется через Keycloak.
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
    public String index(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            return "redirect:/account";
        }
        return "redirect:/login";
    }

    /**
     * GET /account - главная страница после авторизации.
     */
    @GetMapping("/account")
    public String accountPage(
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal Jwt jwt,
            Model model) {

        if (oidcUser != null) {
            log.info("User authenticated via OIDC: {}", oidcUser.getPreferredUsername());
            model.addAttribute("username", oidcUser.getPreferredUsername());
            model.addAttribute("email", oidcUser.getEmail());
            model.addAttribute("fullName", oidcUser.getFullName());
            model.addAttribute("attributes", oidcUser.getAttributes());
        } else if (jwt != null) {
            log.info("User authenticated via JWT: {}", jwt.getSubject());
            model.addAttribute("username", jwt.getClaimAsString("preferred_username"));
            model.addAttribute("email", jwt.getClaimAsString("email"));
            model.addAttribute("fullName", jwt.getClaimAsString("name"));
            model.addAttribute("attributes", jwt.getClaims());
        }

        return "main";
    }

    /**
     * GET /user-info - информация о текущем пользователе для frontend.
     */
    @GetMapping("/user-info")
    public Map<String, Object> userInfo(
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal Jwt jwt) {

        if (oidcUser != null) {
            return Map.of(
                "username", oidcUser.getPreferredUsername(),
                "email", oidcUser.getEmail(),
                "fullName", oidcUser.getFullName(),
                "attributes", oidcUser.getAttributes()
            );
        } else if (jwt != null) {
            return Map.of(
                "username", jwt.getClaimAsString("preferred_username"),
                "email", jwt.getClaimAsString("email"),
                "fullName", jwt.getClaimAsString("name"),
                "attributes", jwt.getClaims()
            );
        }

        return Map.of("authenticated", false);
    }
}
