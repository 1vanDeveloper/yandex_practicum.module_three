package ru.yandex.practicum.cash.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.dto.NotificationRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class NotificationsClient {

    private final RestClient restClient;
    private final Executor executor;
    private final String notificationsServiceUrl;

    public NotificationsClient(Executor asyncExecutor,
                               @Value("${notifications.service.url:http://notifications:8080}") String notificationsServiceUrl) {
        this.restClient = RestClient.create();
        this.executor = asyncExecutor;
        this.notificationsServiceUrl = notificationsServiceUrl;
    }

    public CompletableFuture<Void> sendNotification(NotificationRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = notificationsServiceUrl + "/notifications/notificate";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "message", request.message()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
