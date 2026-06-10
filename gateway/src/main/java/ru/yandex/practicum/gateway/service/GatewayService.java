package ru.yandex.practicum.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.gateway.client.AccountsClient;
import ru.yandex.practicum.gateway.client.CashClient;
import ru.yandex.practicum.gateway.client.TransferClient;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.CashAction;
import ru.yandex.practicum.gateway.dto.CashRequest;
import ru.yandex.practicum.gateway.dto.RegisterRequest;
import ru.yandex.practicum.gateway.dto.TransferRequest;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    @Value("${services.accounts.url:http://localhost:8082}")
    private String accountsUrl;

    @Value("${services.cash.url:http://localhost:8084}")
    private String cashUrl;

    @Value("${services.transfer.url:http://localhost:8085}")
    private String transferUrl;

    public CompletableFuture<AccountResponse> getAccount(String login) {
        log.info("Gateway: getting account for login: {}", login);
        return accountsClient.getAccount(accountsUrl, login);
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        log.info("Gateway: registering new account for login: {}", request.getLogin());
        return accountsClient.register(accountsUrl, request);
    }

    public CompletableFuture<AccountResponse> updateAccount(String login, UpdateAccountRequest request) {
        log.info("Gateway: updating account for login: {}", login);
        return accountsClient.updateAccount(accountsUrl, request);
    }

    public CompletableFuture<Void> processCash(String login, Integer amount, CashAction action) {
        log.info("Gateway: processing cash action: {} for login: {}, amount: {}", action, login, amount);
        CashRequest request = CashRequest.builder()
                .login(login)
                .amount(amount)
                .build();

        if (action == CashAction.PUT) {
            return cashClient.deposit(cashUrl, request);
        } else {
            return cashClient.withdraw(cashUrl, request);
        }
    }

    public CompletableFuture<Void> processTransfer(String fromLogin, String toLogin, Integer amount) {
        log.info("Gateway: processing transfer from {} to {} amount: {}", fromLogin, toLogin, amount);
        TransferRequest request = TransferRequest.builder()
                .fromLogin(fromLogin)
                .toLogin(toLogin)
                .amount(amount)
                .build();
        return transferClient.createTransfer(transferUrl, request);
    }
}
