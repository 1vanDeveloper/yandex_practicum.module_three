package ru.yandex.practicum.transfer.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие нотификации для отправки в Kafka из Transfer сервиса.
 * Immutable record для передачи через Kafka.
 */
public record TransferNotificationEvent(
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
    public static TransferNotificationEvent create(String login, String message, String type) {
        return new TransferNotificationEvent(
                UUID.randomUUID().toString(),
                login,
                login,
                message,
                type,
                Instant.now()
        );
    }
}
