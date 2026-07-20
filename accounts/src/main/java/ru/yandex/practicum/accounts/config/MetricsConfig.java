package ru.yandex.practicum.accounts.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация метрик для accounts сервиса.
 */
@Configuration
public class MetricsConfig {

    /**
     * Счётчик неуспешных попыток снятия денег (с группировкой по логину).
     */
    @Bean
    public Counter cashWithdrawalFailedCounter(MeterRegistry registry) {
        return Counter.builder("cash_withdrawal_failed_total")
                .description("Количество неуспешных попыток снятия денег")
                .tag("service", "accounts")
                .register(registry);
    }

    /**
     * Счётчик неуспешных попыток перевода денег (с группировкой по логинам).
     */
    @Bean
    public Counter transferFailedCounter(MeterRegistry registry) {
        return Counter.builder("transfer_failed_total")
                .description("Количество неуспешных попыток перевода денег")
                .tag("service", "accounts")
                .register(registry);
    }
}
