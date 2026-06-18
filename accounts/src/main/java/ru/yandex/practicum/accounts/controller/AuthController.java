package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accounts.dto.JwtTokenResponse;
import ru.yandex.practicum.accounts.dto.LoginRequest;
import ru.yandex.practicum.accounts.dto.RegisterRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.service.AuthService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<ResponseEntity<Account>> register(@Valid @RequestBody RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Account account = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        });
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<JwtTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            JwtTokenResponse tokenResponse = authService.login(request);
            return ResponseEntity.ok(tokenResponse);
        });
    }
}
