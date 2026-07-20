package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${kafka.topic.notifications:notifications.events}")
    private String topic;

    /**
     * Отправляет событие нотификации в Kafka топик (синхронно).
     * Блокирует поток до подтверждения отправки Kafka.
     *
     * @param event событие для отправки
     * @throws RuntimeException если отправка не удалась
     */
    public void sendNotificationSync(NotificationEvent event) {
        log.info("Синхронная отправка события в Kafka: topic={}, event={}", topic, event);
        try {
            SendResult<String, NotificationEvent> result =
                    kafkaTemplate.send(topic, event.login(), event).get();
            log.info("Событие успешно отправлено в Kafka: topic={}, partition={}, offset={}",
                    topic,
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
