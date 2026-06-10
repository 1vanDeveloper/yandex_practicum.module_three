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

    public CompletableFuture<AccountResponse> getAccount(String accountsUrl, String login) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                    accountsUrl + "/accounts/{login}", AccountResponse.class, login);
            return response.getBody();
        });
    }

    public CompletableFuture<Void> register(String accountsUrl, RegisterRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(accountsUrl + "/accounts", request, Void.class);
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(String accountsUrl, UpdateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                    accountsUrl + "/accounts", request, AccountResponse.class);
            return response.getBody();
        });
    }
}
