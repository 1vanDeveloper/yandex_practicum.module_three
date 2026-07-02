package ru.yandex.practicum.cash.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Событие нотификации для отправки в Kafka из Cash сервиса.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashNotificationEvent {

    private String id;
    private String accountId;
    private String login;
    private String message;
    private String type;
    private Instant timestamp;

    /**
     * Создаёт событие с автоматически сгенерированными ID и timestamp.
     */
    public static CashNotificationEvent create(String login, String message, String type) {
        return CashNotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId(login)
                .login(login)
                .message(message)
                .type(type)
                .timestamp(Instant.now())
                .build();
    }
}
