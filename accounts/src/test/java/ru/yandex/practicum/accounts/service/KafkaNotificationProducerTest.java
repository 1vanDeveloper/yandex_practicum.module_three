package ru.yandex.practicum.accounts.service;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.yandex.practicum.accounts.event.NotificationEvent;

import java.lang.reflect.Field;
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

    private static final String TEST_TOPIC = "notifications.events";

    @BeforeEach
    void setUp() throws Exception {
        kafkaTemplate = mock(KafkaTemplate.class);
        kafkaProducer = new KafkaNotificationProducer(kafkaTemplate);
        // Inject test topic via reflection since @Value doesn't work in plain unit tests
        Field topicField = KafkaNotificationProducer.class.getDeclaredField("topic");
        topicField.setAccessible(true);
        topicField.set(kafkaProducer, TEST_TOPIC);
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
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotificationSync(event);

        // Assert
        verify(kafkaTemplate).send(eq(TEST_TOPIC), eq(event.login()), eq(event));
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
        when(kafkaTemplate.send(eq(TEST_TOPIC), anyString(), any())).thenReturn(future);

        // Act
        kafkaProducer.sendNotificationSync(event);

        // Assert
        verify(kafkaTemplate).send(eq(TEST_TOPIC), eq("test@example.com"), any());
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
