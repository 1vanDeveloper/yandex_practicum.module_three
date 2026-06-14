package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class AccountsClient {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final Executor executor;

    public AccountsClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.executor = Executors.newCachedThreadPool();
    }

    public CompletableFuture<Void> debitAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/debit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", login, "amount", amount))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> creditAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/credit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", login, "amount", amount))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
