package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accounts.dto.AccountBrief;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public CompletableFuture<AccountResponse> getMyAccount(
            org.springframework.security.core.Authentication authentication) {
        String login = authentication.getName();
        log.info("GET /accounts/me: login={}", login);
        return accountService.getAccountByLogin(login);
    }

    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<AccountResponse> updateMyAccount(
            @Valid @RequestBody UpdateAccountRequest request,
            org.springframework.security.core.Authentication authentication) {
        String login = authentication.getName();
        log.info("PUT /accounts/me: login={}", login);
        return accountService.updateAccount(login, request);
    }

    @GetMapping("/{login}")
    public CompletableFuture<AccountResponse> getAccount(@PathVariable String login) {
        return accountService.getAccountByLogin(login);
    }

    @GetMapping
    public CompletableFuture<List<AccountBrief>> getAllAccounts() {
        return accountService.getAllAccounts();
    }
}
