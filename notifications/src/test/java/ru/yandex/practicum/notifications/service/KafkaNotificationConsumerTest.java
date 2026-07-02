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
        NotificationEvent event = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId("account-123")
                .login("user@example.com")
                .message("Test message")
                .type("ACCOUNT_NOTIFICATION")
                .timestamp(Instant.now())
                .build();

        // Act
        kafkaConsumer.consumeNotification(event);

        // Assert
        verify(notificationService).saveNotification(eq(event));
    }

    @Test
    void consumeNotification_shouldProcessEventWithCorrectMessage() {
        // Arrange
        NotificationEvent event = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId("account-456")
                .login("test@example.com")
                .message("Another test message")
                .type("ACCOUNT_NOTIFICATION")
                .timestamp(Instant.now())
                .build();

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
