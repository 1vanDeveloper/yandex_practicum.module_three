package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxNotificationRepository outboxRepository;

    /**
     * Сохраняет сообщение в outbox с детерминированным idempotencyKey.
     * Ключ строится из типа события и идентификатора сущности для защиты от дублей.
     *
     * @param login логин пользователя
     * @param message сообщение
     * @param eventType тип события (например, "account-created", "account-updated")
     * @param entityId идентификатор сущности (например, login пользователя)
     */
    @Transactional
    public OutboxMessage saveMessage(String login, String message, String eventType, String entityId) {
        // Детерминированный ключ идемпотентности: тип-сущности:entity-id
        String idempotencyKey = eventType + ":" + entityId;

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .idempotencyKey(idempotencyKey)
                .login(login)
                .message(message)
                .status(OutboxMessage.Status.PENDING.getValue())
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return outboxRepository.save(outboxMessage);
    }
}
