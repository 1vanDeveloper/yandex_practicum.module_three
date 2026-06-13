package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.RegisterRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.service.AuthService;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для регистрации пользователей.
 * Аутентификация осуществляется через Keycloak (OAuth2).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/register - регистрация нового пользователя.
     * Создает пользователя в локальной БД Accounts сервиса.
     * После регистрации пользователь должен войти через Keycloak.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<ResponseEntity<Account>> register(@Valid @RequestBody RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Account account = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        });
    }
}
