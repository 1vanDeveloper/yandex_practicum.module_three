package ru.yandex.practicum.cash.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие нотификации для отправки в Kafka из Cash сервиса.
 * Immutable record для передачи через Kafka.
 */
public record CashNotificationEvent(
        String id,
        String accountId,
        String login,
        String message,
        String type,
        Instant timestamp
) {
    /**
     * Создаёт событие с автоматически сгенерированными ID и timestamp.
     */
    public static CashNotificationEvent create(String login, String message, String type) {
        return new CashNotificationEvent(
                UUID.randomUUID().toString(),
                login,
                login,
                message,
                type,
                Instant.now()
        );
    }
}
