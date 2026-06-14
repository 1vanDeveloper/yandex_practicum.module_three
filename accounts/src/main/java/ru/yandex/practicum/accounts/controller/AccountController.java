package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accounts.dto.AccountBrief;
import ru.yandex.practicum.accounts.dto.AccountIdResponse;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CreateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<AccountIdResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<AccountResponse> register(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request)
                .thenCompose(response -> accountService.getAccountByLogin(request.getLogin()));
    }

    @GetMapping("/{login}")
    public CompletableFuture<AccountResponse> getAccount(@PathVariable String login) {
        return accountService.getAccountByLogin(login);
    }

    @PutMapping("/{login}")
    public CompletableFuture<AccountResponse> updateAccount(
            @PathVariable String login,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(login, request);
    }

    @GetMapping
    public CompletableFuture<List<AccountBrief>> getAllAccounts() {
        return accountService.getAllAccounts();
    }
}
