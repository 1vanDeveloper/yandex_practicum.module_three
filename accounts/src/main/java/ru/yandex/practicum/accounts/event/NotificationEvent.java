package ru.yandex.practicum.accounts.event;

import java.time.Instant;

/**
 * Событие нотификации для отправки в Kafka.
 * Immutable record для передачи через Kafka.
 */
public record NotificationEvent(
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
    public static NotificationEvent create(String login, String message, String type) {
        return new NotificationEvent(
                java.util.UUID.randomUUID().toString(),
                login,
                login,
                message,
                type,
                Instant.now()
        );
    }
}
