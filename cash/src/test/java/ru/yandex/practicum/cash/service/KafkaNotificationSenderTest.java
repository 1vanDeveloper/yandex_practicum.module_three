package ru.yandex.practicum.cash.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import ru.yandex.practicum.cash.config.KafkaConfig;
import ru.yandex.practicum.cash.event.CashNotificationEvent;

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
    private KafkaTemplate<String, CashNotificationEvent> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void sendNotification_shouldSendMessageToKafkaTopic() throws Exception {
        // Given
        CashNotificationEvent event = CashNotificationEvent.create(
                "test_user",
                "Test notification message",
                "DEPOSIT"
        );

        // Create consumer to verify message was sent
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put("spring.json.type.mapping",
                "ru.yandex.practicum.cash.event.CashNotificationEvent:ru.yandex.practicum.cash.event.CashNotificationEvent");

        Consumer<String, CashNotificationEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JacksonJsonDeserializer<>(CashNotificationEvent.class)
        ).createConsumer();

        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "notifications.events");

        // When
        kafkaNotificationSender.sendNotificationSync(event);

        // Then
        ConsumerRecord<String, CashNotificationEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                "notifications.events",
                Duration.ofSeconds(10)
        );
        assertNotNull(record);
        assertEquals("test_user", record.key());
        assertNotNull(record.value());
        assertEquals("test_user", record.value().getLogin());
        assertEquals("Test notification message", record.value().getMessage());
        assertEquals("DEPOSIT", record.value().getType());
        assertNotNull(record.value().getId());
        assertNotNull(record.value().getTimestamp());

        consumer.close();
    }

    @Test
    void sendNotification_eventHasCorrectFields() {
        // Given
        CashNotificationEvent event = CashNotificationEvent.create(
                "user123",
                "Withdrawal completed: 500.00",
                "WITHDRAW"
        );

        // Then
        assertNotNull(event.getId());
        assertEquals("user123", event.getLogin());
        assertEquals("user123", event.getAccountId());
        assertEquals("Withdrawal completed: 500.00", event.getMessage());
        assertEquals("WITHDRAW", event.getType());
        assertNotNull(event.getTimestamp());
    }
}
