package ru.yandex.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notifications.event.NotificationEvent;

/**
 * Сервис для получения событий нотификаций из Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Обрабатывает события нотификаций из Kafka топика.
     *
     * @param event событие для обработки
     */
    @KafkaListener(topics = "${kafka.topic.notifications:notifications.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNotification(@Payload NotificationEvent event) {
        log.info("Получено событие из Kafka: topic={}, event={}", 
                "notifications.events", event);

        try {
            // Сохраняем уведомление в базу данных
            notificationService.saveNotification(event);
            log.info("Уведомление успешно обработано: eventId={}, login={}", 
                    event.getId(), event.getLogin());
        } catch (Exception e) {
            log.error("Ошибка при обработке уведомления: eventId={}, login={}", 
                    event.getId(), event.getLogin(), e);
            throw e; // Пробрасываем исключение для обработки Kafka
        }
    }
}
