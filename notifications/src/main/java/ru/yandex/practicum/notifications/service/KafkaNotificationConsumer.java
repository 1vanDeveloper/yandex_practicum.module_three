package ru.yandex.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notifications.event.NotificationEvent;

import java.util.Map;

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
    public void consumeNotification(@Payload Map<String, Object> event) {
        log.info("Получено событие из Kafka: topic={}, event={}",
                "notifications.events", event);

        try {
            // Конвертируем Map в NotificationEvent
            NotificationEvent notificationEvent = new NotificationEvent();
            notificationEvent.setId((String) event.get("id"));
            notificationEvent.setLogin((String) event.get("login"));
            notificationEvent.setMessage((String) event.get("message"));
            notificationEvent.setType((String) event.get("type"));
            notificationEvent.setAccountId((String) event.get("accountId"));

            // Сохраняем уведомление в базу данных
            notificationService.saveNotification(notificationEvent);
            log.info("Уведомление успешно обработано: eventId={}, login={}",
                    notificationEvent.getId(), notificationEvent.getLogin());
        } catch (Exception e) {
            log.error("Ошибка при обработке уведомления: event={}", event, e);
            throw e; // Пробрасываем исключение для обработки Kafka
        }
    }
}
