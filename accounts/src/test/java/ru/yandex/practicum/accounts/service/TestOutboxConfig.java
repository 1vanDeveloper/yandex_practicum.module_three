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
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    @Primary
    public NotificationsClient testNotificationsClient() {
        return new NotificationsClient(RestClient.create(), null) {
            @Override
            public void sendNotification(String notificationsUrl, NotificationRequest request) {
                // Mock successful notification send
            }
        };
    }
}
