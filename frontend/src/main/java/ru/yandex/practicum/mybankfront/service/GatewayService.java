package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybankfront.client.GatewayClient;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final GatewayClient gatewayClient;

    @Value("${gateway.service.url:http://localhost:8086}")
    private String gatewayUrl;

    public CompletableFuture<Void> register(RegisterRequest request) {
        log.info("Frontend: registering new account for login: {}, email: {}", request.getLogin(), request.getEmail());
        return gatewayClient.register(gatewayUrl, request);
    }

    public CompletableFuture<String> login(LoginRequest request) {
        log.info("Frontend: logging in user: {}", request.getUsername());
        return gatewayClient.login(gatewayUrl, request);
    }

    public CompletableFuture<AccountResponse> getAccount(String login) {
        log.info("Frontend: getting account for login: {}", login);
        return gatewayClient.getAccount(gatewayUrl, login);
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String login,
            String firstName,
            String lastName,
            String birthDate) {
        log.info("Frontend: updating account for login: {}", login);
        return gatewayClient.updateAccount(gatewayUrl, firstName, lastName, birthDate);
    }

    public CompletableFuture<Void> processCash(String login, Integer value, String action) {
        log.info("Frontend: processing cash action: {} for login: {}, value: {}", action, login, value);
        return gatewayClient.processCash(gatewayUrl, value, action);
    }

    public CompletableFuture<Void> processTransfer(String fromLogin, Integer value, String toLogin) {
        log.info("Frontend: processing transfer from {} to {} value: {}", fromLogin, toLogin, value);
        return gatewayClient.processTransfer(gatewayUrl, value, toLogin);
    }
}
