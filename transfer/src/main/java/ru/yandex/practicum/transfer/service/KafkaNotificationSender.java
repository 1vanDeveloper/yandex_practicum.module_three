package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.event.TransferNotificationEvent;

/**
 * Сервис для отправки событий нотификаций в Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationSender {

    private final KafkaTemplate<String, TransferNotificationEvent> kafkaTemplate;

    @Value("${kafka.topic.notifications:notifications.events}")
    private String notificationsTopic;

    /**
     * Отправляет событие нотификации в Kafka топик (синхронно).
     * Блокирует поток до подтверждения отправки Kafka.
     *
     * @param event событие для отправки
     * @throws RuntimeException если отправка не удалась
     */
    public void sendNotificationSync(TransferNotificationEvent event) {
        log.info("Синхронная отправка события в Kafka: topic={}, event={}", notificationsTopic, event);
        try {
            SendResult<String, TransferNotificationEvent> result =
                    kafkaTemplate.send(notificationsTopic, event.login(), event).get();
            log.info("Событие успешно отправлено в Kafka: topic={}, partition={}, offset={}, eventId={}",
                    notificationsTopic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    event.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Отправка в Kafka прервана: eventId={}", event.id(), e);
            throw new RuntimeException("Отправка в Kafka прервана", e);
        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Ошибка при синхронной отправке в Kafka: eventId={}", event.id(), e);
            throw new RuntimeException("Ошибка при отправке в Kafka", e);
        }
    }
}
