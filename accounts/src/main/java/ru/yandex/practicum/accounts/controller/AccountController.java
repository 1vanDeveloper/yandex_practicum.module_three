package ru.yandex.practicum.accounts.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accounts.dto.AccountBrief;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{login}")
    public CompletableFuture<AccountResponse> getAccount(@PathVariable String login) {
        return accountService.getAccountByLogin(login);
    }

    @GetMapping
    public CompletableFuture<List<AccountBrief>> getAllAccounts() {
        return accountService.getAllAccounts();
    }
}
