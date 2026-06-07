package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.transfer.dto.NotificationRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class NotificationsClient {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public NotificationsClient(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<Void> sendNotification(NotificationRequest request, String bearerToken) {
        return CompletableFuture.supplyAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("notifications-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("notifications-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/notifications";

            Map<String, String> body = Map.of(
                    "login", request.login(),
                    "message", request.message()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);
            headers.set("Content-Type", "application/json");

            RequestEntity<Map<String, String>> requestEntity = new RequestEntity<>(body, headers, HttpMethod.POST, URI.create(url));
            ResponseEntity<Void> response = restTemplate.exchange(requestEntity, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to send notification: " + response.getStatusCode());
            }
            return null;
        });
    }
}
