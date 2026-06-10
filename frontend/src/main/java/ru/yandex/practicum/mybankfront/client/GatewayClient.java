package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class GatewayClient {

    private final RestTemplate restTemplate;

    public CompletableFuture<Void> register(String gatewayUrl, RegisterRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(gatewayUrl + "/gateway/register", request, Void.class);
        });
    }

    public CompletableFuture<AccountResponse> getAccount(String gatewayUrl, String login) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                    gatewayUrl + "/gateway/account", AccountResponse.class);
            return response.getBody();
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String gatewayUrl,
            String firstName,
            String lastName,
            String birthDate) {
        return CompletableFuture.supplyAsync(() -> {
            ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                    gatewayUrl + "/gateway/account",
                    new UpdateAccountRequest(firstName, lastName, birthDate),
                    AccountResponse.class);
            return response.getBody();
        });
    }

    public CompletableFuture<Void> processCash(
            String gatewayUrl,
            Integer value,
            String action) {
        return CompletableFuture.runAsync(() -> {
            String url = gatewayUrl + "/gateway/cash?value=" + value + "&action=" + action;
            restTemplate.postForEntity(url, null, Void.class);
        });
    }

    public CompletableFuture<Void> processTransfer(
            String gatewayUrl,
            Integer value,
            String login) {
        return CompletableFuture.runAsync(() -> {
            String url = gatewayUrl + "/gateway/transfer?value=" + value + "&login=" + login;
            restTemplate.postForEntity(url, null, Void.class);
        });
    }

    private record UpdateAccountRequest(String firstName, String lastName, String birthDate) {}
}
