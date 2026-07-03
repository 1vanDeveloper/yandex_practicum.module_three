package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.event.NotificationEvent;

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
     * Отправляет событие нотификации в Kafka топик (синхронно).
     * Блокирует поток до подтверждения отправки Kafka.
     *
     * @param event событие для отправки
     * @throws RuntimeException если отправка не удалась
     */
    public void sendNotificationSync(NotificationEvent event) {
        log.info("Синхронная отправка события в Kafka: topic={}, event={}", TOPIC, event);
        try {
            SendResult<String, NotificationEvent> result =
                    kafkaTemplate.send(TOPIC, event.getLogin(), event).get();
            log.info("Событие успешно отправлено в Kafka: topic={}, partition={}, offset={}",
                    TOPIC,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Отправка в Kafka прервана: event={}", event, e);
            throw new RuntimeException("Отправка в Kafka прервана", e);
        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Ошибка при синхронной отправке в Kafka: event={}", event, e);
            throw new RuntimeException("Ошибка при отправке в Kafka", e);
        }
    }
}
