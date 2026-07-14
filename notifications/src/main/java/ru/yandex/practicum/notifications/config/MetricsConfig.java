package ru.yandex.practicum.notifications.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация метрик для notifications сервиса.
 */
@Configuration
public class MetricsConfig {

    /**
     * Счётчик неуспешных попыток отправки уведомлений (с группировкой по логину).
     */
    @Bean
    public Counter notificationSendFailedCounter(MeterRegistry registry) {
        return Counter.builder("notification_send_failed_total")
                .description("Количество неуспешных попыток отправки уведомлений")
                .tag("service", "notifications")
                .register(registry);
    }
}
