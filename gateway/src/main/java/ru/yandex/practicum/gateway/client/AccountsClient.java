package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.RegisterRequest;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestTemplate restTemplate;

    private static final String SERVICE_NAME = "accounts-service";

    public CompletableFuture<AccountResponse> getAccount(String login) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                    "http://" + SERVICE_NAME + "/accounts/{login}", AccountResponse.class, login);
            return response.getBody();
        });
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity("http://" + SERVICE_NAME + "/accounts", request, Void.class);
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(UpdateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                    "http://" + SERVICE_NAME + "/accounts", request, AccountResponse.class);
            return response.getBody();
        });
    }
}
