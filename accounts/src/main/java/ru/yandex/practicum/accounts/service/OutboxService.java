package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxNotificationRepository outboxRepository;

    @Transactional
    public CompletableFuture<OutboxMessage> saveMessage(String login, String message) {
        return CompletableFuture.supplyAsync(() -> {
            OutboxMessage outboxMessage = OutboxMessage.builder()
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
