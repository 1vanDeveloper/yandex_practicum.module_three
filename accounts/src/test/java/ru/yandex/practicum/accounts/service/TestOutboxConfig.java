package ru.yandex.practicum.accounts.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;

@TestConfiguration
public class TestOutboxConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Primary
    public NotificationsClient testNotificationsClient(RestClient.Builder restClientBuilder) {
        return new NotificationsClient(restClientBuilder, null) {
            @Override
            public void sendNotification(String notificationsUrl, NotificationRequest request) {
                // Mock successful notification send
            }
        };
    }
}
