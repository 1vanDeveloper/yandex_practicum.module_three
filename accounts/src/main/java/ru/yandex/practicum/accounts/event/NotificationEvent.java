package ru.yandex.practicum.accounts.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Событие нотификации для отправки в Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String id;
    private String accountId;
    private String login;
    private String message;
    private String type;
    private Instant timestamp;
}
