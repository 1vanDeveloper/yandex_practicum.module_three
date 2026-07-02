package ru.yandex.practicum.accounts.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.yandex.practicum.accounts.service.KafkaNotificationProducer;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация для отключения Kafka в тестах.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    public KafkaNotificationProducer kafkaNotificationProducer() {
        return mock(KafkaNotificationProducer.class);
    }
}
