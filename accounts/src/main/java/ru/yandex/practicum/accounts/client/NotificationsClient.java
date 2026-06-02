package ru.yandex.practicum.accounts.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final RestClient.Builder restClientBuilder;

    public CompletableFuture<Void> sendNotification(String notificationsUrl, NotificationRequest request) {
        return CompletableFuture.runAsync(() -> {
            restClientBuilder.build()
                    .post()
                    .uri(notificationsUrl + "/notifications/notificate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        });
    }
}
