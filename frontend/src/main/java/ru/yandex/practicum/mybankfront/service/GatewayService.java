package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mybankfront.client.GatewayClient;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final GatewayClient gatewayClient;

    public CompletableFuture<AccountResponse> getAccount() {
        log.info("Frontend: getting account for authenticated user");
        return gatewayClient.getAccount();
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String firstName,
            String lastName,
            String birthDate) {
        log.info("Frontend: updating account for authenticated user");
        return gatewayClient.updateAccount(firstName, lastName, birthDate);
    }

    public CompletableFuture<Void> processCash(Integer value, String action) {
        log.info("Frontend: processing cash action: {} for authenticated user, value: {}", action, value);
        return gatewayClient.processCash(value, action);
    }

    public CompletableFuture<Void> processTransfer(Integer value, String toLogin) {
        log.info("Frontend: processing transfer to {} value: {}", toLogin, value);
        return gatewayClient.processTransfer(value, toLogin);
    }
}
