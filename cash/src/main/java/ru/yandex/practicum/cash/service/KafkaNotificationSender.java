package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.event.CashNotificationEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис для отправки событий нотификаций в Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationSender {

    private final KafkaTemplate<String, CashNotificationEvent> kafkaTemplate;

    @Value("${kafka.topic.notifications:notifications.events}")
    private String notificationsTopic;

    /**
     * Отправляет событие нотификации в Kafka топик.
     *
     * @param event событие для отправки
     * @return CompletableFuture с результатом отправки
     */
    public CompletableFuture<SendResult<String, CashNotificationEvent>> sendNotification(CashNotificationEvent event) {
        log.info("Отправка события в Kafka: topic={}, event={}", notificationsTopic, event);

        CompletableFuture<SendResult<String, CashNotificationEvent>> future = kafkaTemplate.send(notificationsTopic, event.getLogin(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Событие успешно отправлено в Kafka: topic={}, partition={}, offset={}, eventId={}",
                        notificationsTopic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getId());
            } else {
                log.error("Ошибка при отправке события в Kafka: topic={}, eventId={}",
                        notificationsTopic, event.getId(), ex);
            }
        });

        return future;
    }

    /**
     * Отправляет событие нотификации в Kafka топик (синхронно).
     *
     * @param event событие для отправки
     */
    public void sendNotificationSync(CashNotificationEvent event) {
        log.info("Синхронная отправка события в Kafka: topic={}, event={}", notificationsTopic, event);
        kafkaTemplate.send(notificationsTopic, event.getLogin(), event);
        log.info("Событие отправлено в Kafka: eventId={}", event.getId());
    }
}
