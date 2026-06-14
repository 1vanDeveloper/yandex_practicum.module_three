package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybankfront.client.GatewayClient;
import ru.yandex.practicum.mybankfront.dto.AccountBrief;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.JwtTokenResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final GatewayClient gatewayClient;

    public CompletableFuture<JwtTokenResponse> login(LoginRequest request) {
        log.info("Frontend: logging in user: {}", request.getLogin());
        return gatewayClient.login(request);
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        log.info("Frontend: registering user: {}", request.getLogin());
        return gatewayClient.register(request);
    }

    public CompletableFuture<AccountResponse> getAccount(String jwtToken) {
        log.info("Frontend: getting account with provided token");
        return gatewayClient.getAccount(jwtToken);
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String firstName,
            String lastName,
            String birthDate,
            String jwtToken) {
        log.info("Frontend: updating account with provided token");
        return gatewayClient.updateAccount(firstName, lastName, birthDate, jwtToken);
    }

    public CompletableFuture<Void> processCash(Integer value, String action, String jwtToken) {
        log.info("Frontend: processing cash action: {} with provided token", action);
        return gatewayClient.processCash(value, action, jwtToken);
    }

    public CompletableFuture<Void> processTransfer(Integer value, String toLogin, String jwtToken) {
        log.info("Frontend: processing transfer to {} with provided token", toLogin);
        return gatewayClient.processTransfer(value, toLogin, jwtToken);
    }

    public CompletableFuture<List<AccountBrief>> getAccountBriefs(String jwtToken) {
        log.info("Frontend: getting account briefs with provided token");
        return gatewayClient.getAccountBriefs(jwtToken);
    }
}
