package ru.yandex.practicum.accounts.service;

import org.apache.kafka.clients.producer.RecordMetadata;
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
    void sendNotificationSync_shouldSendToCorrectTopic() throws Exception {
        // Arrange
        NotificationEvent event = createTestEvent();
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(1L);

        SendResult<String, NotificationEvent> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        CompletableFuture<SendResult<String, NotificationEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("notifications.events"), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotificationSync(event);

        // Assert
        verify(kafkaTemplate).send(eq("notifications.events"), eq(event.getLogin()), eq(event));
    }

    @Test
    void sendNotificationSync_shouldUseLoginAsKey() throws Exception {
        // Arrange
        NotificationEvent event = createTestEvent();
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(1L);

        SendResult<String, NotificationEvent> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        CompletableFuture<SendResult<String, NotificationEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("notifications.events"), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotificationSync(event);

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
