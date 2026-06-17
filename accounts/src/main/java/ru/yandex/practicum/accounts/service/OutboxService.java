package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxNotificationRepository outboxRepository;

    @Transactional
    public OutboxMessage saveMessage(String login, String message) {
        // Генерируем idempotency key на основе логина и сообщения для дедупликации
        String idempotencyKey = generateIdempotencyKey(login, message);

        // Проверяем, нет ли уже сообщения с таким ключом (защита от дубликатов)
        OutboxMessage existing = outboxRepository.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }

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

    /**
     * Генерирует уникальный ключ идемпотентности для дедупликации сообщений.
     * Комбинирует login, message и timestamp для уникальности.
     */
    private String generateIdempotencyKey(String login, String message) {
        return login + ":" + message + ":" + System.currentTimeMillis();
    }
}
