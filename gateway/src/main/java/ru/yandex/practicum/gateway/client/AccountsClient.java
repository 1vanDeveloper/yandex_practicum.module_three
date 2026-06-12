package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.RegisterRequest;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestTemplate restTemplate;
    private final RestTemplate publicRestTemplate;

    @Value("${services.accounts.url:http://accounts-service:8080}")
    private String accountsServiceUrl;

    public CompletableFuture<AccountResponse> getAccount(String login) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                    accountsServiceUrl + "/accounts/{login}", AccountResponse.class, login);
            return response.getBody();
        });
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        return CompletableFuture.runAsync(() -> {
            publicRestTemplate.postForEntity(accountsServiceUrl + "/auth/register", request, Void.class);
        });
    }

    public CompletableFuture<Map<String, Object>> login(String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> credentials = Map.of("login", login, "password", password);
            ResponseEntity<Map> response = publicRestTemplate.postForEntity(
                    accountsServiceUrl + "/auth/login", credentials, Map.class);
            return response.getBody();
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(String login, UpdateAccountRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            restTemplate.put(accountsServiceUrl + "/accounts/{login}", request, login);
            return restTemplate.getForEntity(
                    accountsServiceUrl + "/accounts/{login}", AccountResponse.class, login).getBody();
        });
    }
}
