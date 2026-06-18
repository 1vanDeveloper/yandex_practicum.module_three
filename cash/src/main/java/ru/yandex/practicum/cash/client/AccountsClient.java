package ru.yandex.practicum.cash.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.WithdrawRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class AccountsClient {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final Executor executor;

    public AccountsClient(DiscoveryClient discoveryClient, Executor asyncExecutor) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.executor = asyncExecutor;
    }

    public CompletableFuture<Void> deposit(DepositRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/deposit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "amount", request.amount()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> withdraw(WithdrawRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("accounts-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/accounts/internal/withdraw";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "amount", request.amount()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
