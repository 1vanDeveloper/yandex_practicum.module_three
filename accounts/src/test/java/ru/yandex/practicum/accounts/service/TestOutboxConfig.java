package ru.yandex.practicum.accounts.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestOutboxConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Primary
    public NotificationsClient testNotificationsClient(RestClient.Builder restClientBuilder) {
        NotificationsClient mock = mock(NotificationsClient.class);
        doNothing().when(mock).sendNotification(null, null);
        return mock;
    }
}
