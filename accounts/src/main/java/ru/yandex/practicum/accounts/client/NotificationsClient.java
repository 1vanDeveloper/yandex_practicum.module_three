package ru.yandex.practicum.accounts.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private String getAccessToken() {
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("accounts-service")
                        .principal("accounts-service")
                        .build()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain access token for accounts-service");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    public CompletableFuture<Void> sendNotification(String notificationsUrl, NotificationRequest request) {
        return CompletableFuture.runAsync(() -> {
            String token = getAccessToken();
            
            restClient.post()
                    .uri(notificationsUrl + "/notifications/notificate")
                    .headers(headers -> headers.set("Authorization", "Bearer " + token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        });
    }
}
