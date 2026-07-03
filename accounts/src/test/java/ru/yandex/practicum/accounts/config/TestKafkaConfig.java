package ru.yandex.practicum.accounts.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.yandex.practicum.accounts.service.KafkaNotificationProducer;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;

/**
 * Тестовая конфигурация для отключения Kafka в тестах.
 * Мок для синхронной отправки (sendNotificationSync) не бросает исключений.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    public KafkaNotificationProducer kafkaNotificationProducer() {
        KafkaNotificationProducer mock = mock(KafkaNotificationProducer.class);
        doNothing().when(mock).sendNotificationSync(any());
        return mock;
    }
}
