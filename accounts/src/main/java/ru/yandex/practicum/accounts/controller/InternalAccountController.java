package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
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

    @GetMapping("/me")
    public CompletableFuture<AccountResponse> getMyAccount(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authToken) {
        String login = authToken.getToken().getSubject();
        log.info("Internal getMyAccount: login={}", login);
        return accountService.getAccountByLogin(login);
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> deposit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal deposit: login={}, amount={}", request.getLogin(), request.getAmount());
        return accountService.deposit(request.getLogin(), request.getAmount());
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> withdraw(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal withdraw: login={}, amount={}", request.getLogin(), request.getAmount());
        return accountService.withdraw(request.getLogin(), request.getAmount());
    }

    @PostMapping("/debit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> debit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal debit: login={}, amount={}", request.getLogin(), request.getAmount());
        return accountService.debit(request.getLogin(), request.getAmount());
    }

    @PostMapping("/credit")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<Void> credit(@Valid @RequestBody InternalBalanceRequest request) {
        log.info("Internal credit: login={}, amount={}", request.getLogin(), request.getAmount());
        return accountService.credit(request.getLogin(), request.getAmount());
    }
}
