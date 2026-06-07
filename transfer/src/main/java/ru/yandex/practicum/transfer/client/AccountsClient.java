package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class AccountsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public AccountsClient(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<Void> debitAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.supplyAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/debit";

            Map<String, Object> body = Map.of(
                    "login", login,
                    "amount", amount
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);
            headers.set("Content-Type", "application/json");

            RequestEntity<Map<String, Object>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, URI.create(url));
            ResponseEntity<Void> response = restTemplate.exchange(requestEntity, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to debit account: " + response.getStatusCode());
            }
            return null;
        });
    }

    public CompletableFuture<Void> creditAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.supplyAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/credit";

            Map<String, Object> body = Map.of(
                    "login", login,
                    "amount", amount
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);
            headers.set("Content-Type", "application/json");

            RequestEntity<Map<String, Object>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, URI.create(url));
            ResponseEntity<Void> response = restTemplate.exchange(requestEntity, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to credit account: " + response.getStatusCode());
            }
            return null;
        });
    }

    public CompletableFuture<Map<String, Object>> getAccountByLogin(String login, String bearerToken) {
        return CompletableFuture.supplyAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/" + login;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);

            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(url));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(requestEntity, (Class<Map<String, Object>>)(Class<?>)Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to get account: " + response.getStatusCode());
            }
            return response.getBody();
        });
    }
}
