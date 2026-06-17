package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accounts.dto.InternalBalanceRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/accounts/internal")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> deposit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal deposit: login={}, amount={}", request.getLogin(), request.getAmount());
        return CompletableFuture.supplyAsync(() -> accountService.deposit(request.getLogin(), request.getAmount()));
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> withdraw(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal withdraw: login={}, amount={}", request.getLogin(), request.getAmount());
        return CompletableFuture.supplyAsync(() -> accountService.withdraw(request.getLogin(), request.getAmount()));
    }

    @PostMapping("/debit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> debit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal debit: login={}, amount={}", request.getLogin(), request.getAmount());
        return CompletableFuture.supplyAsync(() -> accountService.debit(request.getLogin(), request.getAmount()));
    }

    @PostMapping("/credit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> credit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal credit: login={}, amount={}", request.getLogin(), request.getAmount());
        return CompletableFuture.supplyAsync(() -> accountService.credit(request.getLogin(), request.getAmount()));
    }
}
