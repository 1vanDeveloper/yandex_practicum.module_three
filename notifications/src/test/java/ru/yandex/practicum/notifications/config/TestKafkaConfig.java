package ru.yandex.practicum.notifications.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.yandex.practicum.notifications.service.KafkaNotificationConsumer;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация для отключения Kafka в тестах.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    public KafkaNotificationConsumer kafkaNotificationConsumer() {
        return mock(KafkaNotificationConsumer.class);
    }
}
