package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxNotificationRepository outboxRepository;

    @Async
    public CompletableFuture<OutboxMessage> saveMessage(String login, String message) {
        return CompletableFuture.supplyAsync(() -> {
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .id(UUID.randomUUID())
                    .login(login)
                    .message(message)
                    .status(OutboxMessage.Status.PENDING.getValue())
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return outboxRepository.save(outboxMessage);
        });
    }
}
