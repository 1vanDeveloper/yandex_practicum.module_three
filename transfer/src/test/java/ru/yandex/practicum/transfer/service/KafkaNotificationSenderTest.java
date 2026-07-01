package ru.yandex.practicum.transfer.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.practicum.transfer.config.KafkaConfig;
import ru.yandex.practicum.transfer.event.TransferNotificationEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Kafka notification sender using Embedded Kafka.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        controlledShutdown = false,
        topics = { "notifications.events" }
)
@ContextConfiguration(classes = { KafkaConfig.class, KafkaNotificationSender.class })
class KafkaNotificationSenderTest {

    @Autowired
    private KafkaNotificationSender kafkaNotificationSender;

    @Autowired
    private KafkaTemplate<String, TransferNotificationEvent> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void sendNotification_shouldSendMessageToKafkaTopic() throws Exception {
        // Given
        TransferNotificationEvent event = TransferNotificationEvent.create(
                "test_user",
                "Test notification message",
                "TRANSFER_SENT"
        );

        // Create consumer to verify message was sent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JacksonJsonDeserializer.class);
        consumerProps.put("spring.json.trusted.packages", "*");
        consumerProps.put("spring.json.type.mapping",
                "ru.yandex.practicum.transfer.event.TransferNotificationEvent:ru.yandex.practicum.transfer.event.TransferNotificationEvent");

        Consumer<String, TransferNotificationEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new org.springframework.kafka.support.serializer.JacksonJsonDeserializer<>(TransferNotificationEvent.class)
        ).createConsumer();

        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "notifications.events");

        // When
        kafkaNotificationSender.sendNotificationSync(event);

        // Then
        ConsumerRecord<String, TransferNotificationEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                "notifications.events",
                Duration.ofSeconds(10)
        );
        assertNotNull(record);
        assertEquals("test_user", record.key());
        assertNotNull(record.value());
        assertEquals("test_user", record.value().getLogin());
        assertEquals("Test notification message", record.value().getMessage());
        assertEquals("TRANSFER_SENT", record.value().getType());
        assertNotNull(record.value().getId());
        assertNotNull(record.value().getTimestamp());

        consumer.close();
    }

    @Test
    void sendNotification_eventHasCorrectFields() {
        // Given
        TransferNotificationEvent event = TransferNotificationEvent.create(
                "user123",
                "Money transferred: 500.00 to receiver",
                "TRANSFER_SENT"
        );

        // Then
        assertNotNull(event.getId());
        assertEquals("user123", event.getLogin());
        assertEquals("user123", event.getAccountId());
        assertEquals("Money transferred: 500.00 to receiver", event.getMessage());
        assertEquals("TRANSFER_SENT", event.getType());
        assertNotNull(event.getTimestamp());
    }
}
