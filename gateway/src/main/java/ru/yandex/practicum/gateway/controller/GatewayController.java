package ru.yandex.practicum.gateway.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.CashAction;
import ru.yandex.practicum.gateway.dto.RegisterRequest;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;
import ru.yandex.practicum.gateway.service.GatewayService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final GatewayService gatewayService;

    /**
     * POST /gateway/register.
     * Регистрация нового пользователя.
     */
    @PostMapping("/register")
    public CompletableFuture<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /gateway/register received for login: {}", request.getLogin());
        return gatewayService.register(request);
    }

    /**
     * GET /gateway/account.
     * Получение данных аккаунта текущего пользователя.
     */
    @GetMapping("/account")
    public CompletableFuture<AccountResponse> getAccount(@AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");
        log.info("GET /gateway/account received for login: {}", login);
        return gatewayService.getAccount(login);
    }

    /**
     * POST /gateway/account.
     * Обновление данных текущего пользователя.
     */
    @PostMapping("/account")
    public CompletableFuture<AccountResponse> updateAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountRequest request) {
        String login = jwt.getClaimAsString("preferred_username");
        log.info("POST /gateway/account received for login: {}", login);
        return gatewayService.updateAccount(login, request);
    }

    /**
     * POST /gateway/cash.
     * Пополнение или снятие со счёта.
     */
    @PostMapping("/cash")
    public CompletableFuture<Void> processCash(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("value") Integer value,
            @RequestParam("action") CashAction action) {
        String login = jwt.getClaimAsString("preferred_username");
        log.info("POST /gateway/cash received for login: {}, action: {}, value: {}", login, action, value);
        return gatewayService.processCash(login, value, action);
    }

    /**
     * POST /gateway/transfer.
     * Перевод со счёта текущего пользователя на счёт другого.
     */
    @PostMapping("/transfer")
    public CompletableFuture<Void> processTransfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("value") Integer value,
            @RequestParam("login") String toLogin) {
        String fromLogin = jwt.getClaimAsString("preferred_username");
        log.info("POST /gateway/transfer received from: {} to: {}, value: {}", fromLogin, toLogin, value);
        return gatewayService.processTransfer(fromLogin, toLogin, value);
    }
}
