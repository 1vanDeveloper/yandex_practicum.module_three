package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;
import ru.yandex.practicum.accounts.entity.OutboxMessage;
import ru.yandex.practicum.accounts.repository.OutboxNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxNotificationRepository outboxRepository;
    private final NotificationsClient notificationsClient;

    @Value("${notifications.service.url:http://notifications:8080}")
    private String notificationsServiceUrl;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 10;

    @Transactional
    public void processPendingMessages() {
        List<OutboxMessage> messages = outboxRepository.findPendingMessagesForUpdate(BATCH_SIZE);
        if (messages.isEmpty()) {
            return;
        }

        messages.stream().limit(BATCH_SIZE).forEach(this::processMessage);
    }

    @Transactional
    public void processMessage(OutboxMessage message) {
        try {
            sendToNotificationsService(message);
            updateMessageStatus(message.getId(), OutboxMessage.Status.SENT.getValue(), null);
        } catch (Exception e) {
            handleProcessingError(message, e);
        }
    }

    @CircuitBreaker(name = "notificationsService", fallbackMethod = "sendNotificationFallback")
    private void sendToNotificationsService(OutboxMessage message) {
        NotificationRequest request = new NotificationRequest(message.getLogin(), message.getMessage());
        notificationsClient.sendNotification(notificationsServiceUrl, request);
    }

    @Transactional
    public void updateMessageStatus(UUID id, String status, String errorMessage) {
        try {
            OutboxMessage existing = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Message not found: " + id));

            existing.setStatus(status);
            existing.setErrorMessage(errorMessage);
            existing.setUpdatedAt(LocalDateTime.now());
            if (OutboxMessage.Status.FAILED.getValue().equals(status)) {
                existing.setRetryCount(existing.getRetryCount() + 1);
            }
            outboxRepository.save(existing);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock failed for message {}, skipping (already processed by another instance)", id);
        }
    }

    @Transactional
    public void handleProcessingError(OutboxMessage message, Throwable error) {
        log.error("Failed to process outbox message {}: {}", message.getId(), error.getMessage());

        if (message.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Max retry count reached for message {}, marking as FAILED", message.getId());
            updateMessageStatus(message.getId(), OutboxMessage.Status.FAILED.getValue(), error.getMessage());
        } else {
            log.info("Retrying message {} (attempt {} of {})", message.getId(), message.getRetryCount() + 1, MAX_RETRY_COUNT);
            updateMessageStatus(message.getId(), OutboxMessage.Status.PENDING.getValue(), error.getMessage());
        }
    }
}
