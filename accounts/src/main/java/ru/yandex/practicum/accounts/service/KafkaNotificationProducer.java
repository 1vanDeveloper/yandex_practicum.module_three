package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.event.NotificationEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис для отправки событий нотификаций в Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    private static final String TOPIC = "notifications.events";

    /**
     * Отправляет событие нотификации в Kafka топик.
     *
     * @param event событие для отправки
     */
    public void sendNotification(NotificationEvent event) {
        log.info("Отправка события в Kafka: topic={}, event={}", TOPIC, event);

        CompletableFuture<SendResult<String, NotificationEvent>> future = kafkaTemplate.send(TOPIC, event.getLogin(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Событие успешно отправлено в Kafka: topic={}, partition={}, offset={}",
                        TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Ошибка при отправке события в Kafka: topic={}, event={}", TOPIC, event, ex);
            }
        });
    }
}
