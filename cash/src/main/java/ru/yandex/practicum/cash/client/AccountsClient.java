package ru.yandex.practicum.cash.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.WithdrawRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class AccountsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public AccountsClient(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    public void deposit(DepositRequest request, String bearerToken) {
        List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
        if (instances.isEmpty()) {
            throw new RuntimeException("accounts-service not found in service discovery");
        }
        ServiceInstance instance = instances.get(0);
        String url = instance.getUri().toString() + "/accounts/internal/deposit";

        Map<String, Object> body = Map.of(
                "login", request.login(),
                "amount", request.amount()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearerToken);
        headers.set("Content-Type", "application/json");

        RequestEntity<Map<String, Object>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, URI.create(url));
        ResponseEntity<Void> response = restTemplate.exchange(requestEntity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to deposit: " + response.getStatusCode());
        }
    }

    public void withdraw(WithdrawRequest request, String bearerToken) {
        List<ServiceInstance> instances = discoveryClient.getInstances("accounts-service");
        if (instances.isEmpty()) {
            throw new RuntimeException("accounts-service not found in service discovery");
        }
        ServiceInstance instance = instances.get(0);
        String url = instance.getUri().toString() + "/accounts/internal/withdraw";

        Map<String, Object> body = Map.of(
                "login", request.login(),
                "amount", request.amount()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearerToken);
        headers.set("Content-Type", "application/json");

        RequestEntity<Map<String, Object>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, URI.create(url));
        ResponseEntity<Void> response = restTemplate.exchange(requestEntity, Void.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to withdraw: " + response.getStatusCode());
        }
    }
}
