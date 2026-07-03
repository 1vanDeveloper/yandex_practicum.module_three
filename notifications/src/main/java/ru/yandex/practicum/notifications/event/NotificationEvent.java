package ru.yandex.practicum.notifications.event;

import java.time.Instant;

/**
 * Событие нотификации для получения из Kafka.
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
}
