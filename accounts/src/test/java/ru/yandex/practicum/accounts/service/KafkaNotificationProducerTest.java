package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.yandex.practicum.accounts.event.NotificationEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для KafkaNotificationProducer с использованием MockProducer.
 */
class KafkaNotificationProducerTest {

    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private KafkaNotificationProducer kafkaProducer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        kafkaProducer = new KafkaNotificationProducer(kafkaTemplate);
    }

    @Test
    void sendNotification_shouldSendToCorrectTopic() {
        // Arrange
        NotificationEvent event = createTestEvent();
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, NotificationEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("notifications.events"), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotification(event);

        // Assert
        verify(kafkaTemplate).send(eq("notifications.events"), eq(event.getLogin()), eq(event));
    }

    @Test
    void sendNotification_shouldUseLoginAsKey() {
        // Arrange
        NotificationEvent event = createTestEvent();
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, NotificationEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("notifications.events"), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotification(event);

        // Assert
        verify(kafkaTemplate).send(eq("notifications.events"), eq("test@example.com"), any());
    }

    private NotificationEvent createTestEvent() {
        return NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId("account-123")
                .login("test@example.com")
                .message("Test notification message")
                .type("ACCOUNT_NOTIFICATION")
                .timestamp(Instant.now())
                .build();
    }
}
