package ru.yandex.practicum.cash.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import ru.yandex.practicum.cash.event.CashNotificationEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Kafka для Cash сервиса.
 * 
 * Внимание: Cash сервис только отправляет сообщения в Kafka (producer).
 * Топик "notifications.events" создаётся в Notifications сервисе (consumer).
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Фабрика продюсеров для отправки сообщений в Kafka.
     */
    @Bean
    public ProducerFactory<String, CashNotificationEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonJsonSerializer<>());
    }

    /**
     * KafkaTemplate для отправки сообщений.
     */
    @Bean
    public KafkaTemplate<String, CashNotificationEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
