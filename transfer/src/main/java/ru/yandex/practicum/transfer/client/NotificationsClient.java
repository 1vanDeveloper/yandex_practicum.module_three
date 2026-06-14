package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transfer.dto.NotificationRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class NotificationsClient {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final Executor executor;

    public NotificationsClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.executor = Executors.newCachedThreadPool();
    }

    public CompletableFuture<Void> sendNotification(NotificationRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            List<ServiceInstance> instances = discoveryClient.getInstances("notifications-service");
            if (instances.isEmpty()) {
                throw new RuntimeException("notifications-service not found in service discovery");
            }
            ServiceInstance instance = instances.get(0);
            String url = instance.getUri().toString() + "/notifications/notificate";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "message", request.message()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
