package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.notifications.event.NotificationEvent;
import ru.yandex.practicum.notifications.exception.KafkaErrorHandler;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit тесты для KafkaNotificationConsumer с использованием MockConsumer.
 */
class KafkaNotificationConsumerTest {

    private NotificationService notificationService;
    private KafkaNotificationConsumer kafkaConsumer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        kafkaConsumer = new KafkaNotificationConsumer(notificationService);
    }

    @Test
    void consumeNotification_shouldSaveNotificationToDatabase() {
        // Arrange
        NotificationEvent event = createTestEvent();

        // Act
        kafkaConsumer.consumeNotification(event);

        // Assert
        verify(notificationService).saveNotification(event);
    }

    @Test
    void consumeNotification_shouldProcessEventWithCorrectLogin() {
        // Arrange
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                "account-123",
                "user@example.com",
                "Test message",
                "ACCOUNT_NOTIFICATION",
                Instant.now()
        );

        // Act
        kafkaConsumer.consumeNotification(event);

        // Assert
        verify(notificationService).saveNotification(eq(event));
    }

    @Test
    void consumeNotification_shouldProcessEventWithCorrectMessage() {
        // Arrange
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                "account-456",
                "test@example.com",
                "Another test message",
                "ACCOUNT_NOTIFICATION",
                Instant.now()
        );

        // Act
        kafkaConsumer.consumeNotification(event);

        // Assert
        verify(notificationService).saveNotification(eq(event));
    }

    @Test
    void consumeNotification_shouldThrowOnServiceError() {
        // Arrange
        NotificationEvent event = createTestEvent();
        doThrow(new RuntimeException("Database error"))
                .when(notificationService).saveNotification(event);

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> kafkaConsumer.consumeNotification(event)
        );
    }

    @Test
    void kafkaErrorHandler_isAckAfterHandle_shouldReturnFalse() {
        // Arrange
        KafkaErrorHandler errorHandler = new KafkaErrorHandler();

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertFalse(errorHandler.isAckAfterHandle());
    }

    private NotificationEvent createTestEvent() {
        return new NotificationEvent(
                UUID.randomUUID().toString(),
                "account-123",
                "test@example.com",
                "Test notification message",
                "ACCOUNT_NOTIFICATION",
                Instant.now()
        );
    }
}
