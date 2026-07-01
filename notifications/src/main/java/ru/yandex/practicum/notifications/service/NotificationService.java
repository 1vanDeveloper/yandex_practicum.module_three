package ru.yandex.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.event.NotificationEvent;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void saveNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .login(event.getLogin())
                .message(event.getMessage())
                .build();
        log.info("Notification saved from Kafka event for user {}: {}", event.getLogin(), event.getMessage());
        notificationRepository.save(notification);
    }
}
