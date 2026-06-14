package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
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
    private final DiscoveryClient discoveryClient;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 10;

    public CompletableFuture<Void> processPendingMessages() {
        return CompletableFuture.supplyAsync(() ->
                outboxRepository.findPendingMessages(BATCH_SIZE))
            .thenCompose(messages -> {
                if (messages.isEmpty()) {
                    return CompletableFuture.completedFuture(null);
                }
                List<CompletableFuture<Void>> futures = messages.stream()
                    .limit(BATCH_SIZE)
                    .map(this::processMessage)
                    .toList();
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            });
    }

    private CompletableFuture<Void> processMessage(OutboxMessage message) {
        return sendToNotificationsService(message)
            .thenCompose(v -> updateMessageStatus(message.getId(), OutboxMessage.Status.SENT.getValue(), null))
            .thenRun(() -> log.info("Successfully processed outbox message: {}", message.getId()))
            .exceptionally(error -> {
                handleProcessingError(message, error);
                return null;
            });
    }

    @CircuitBreaker(name = "notificationsService", fallbackMethod = "sendNotificationFallback")
    private CompletableFuture<Void> sendToNotificationsService(OutboxMessage message) {
        String notificationsUrl = getNotificationsServiceUrl();
        NotificationRequest request = new NotificationRequest(message.getLogin(), message.getMessage());
        return notificationsClient.sendNotification(notificationsUrl, request);
    }

    private CompletableFuture<Void> sendNotificationFallback(OutboxMessage message, Throwable t) {
        log.warn("Circuit breaker opened for notifications service, message {} will be retried later: {}", 
                message.getId(), t.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    private String getNotificationsServiceUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances("notifications-service");
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances of notifications-service found in Consul");
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }

    private CompletableFuture<Void> updateMessageStatus(UUID id, String status, String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            OutboxMessage existing = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Message not found: " + id));
            existing.setStatus(status);
            existing.setErrorMessage(errorMessage);
            existing.setUpdatedAt(LocalDateTime.now());
            if (OutboxMessage.Status.FAILED.getValue().equals(status)) {
                existing.setRetryCount(existing.getRetryCount() + 1);
            }
            outboxRepository.save(existing);
            return null;
        });
    }

    private CompletableFuture<Void> handleProcessingError(OutboxMessage message, Throwable error) {
        log.error("Failed to process outbox message {}: {}", message.getId(), error.getMessage());

        if (message.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Max retry count reached for message {}, marking as FAILED", message.getId());
            return updateMessageStatus(message.getId(), OutboxMessage.Status.FAILED.getValue(), error.getMessage());
        } else {
            log.info("Retrying message {} (attempt {} of {})", message.getId(), message.getRetryCount() + 1, MAX_RETRY_COUNT);
            return updateMessageStatus(message.getId(), OutboxMessage.Status.PENDING.getValue(), error.getMessage());
        }
    }
}
