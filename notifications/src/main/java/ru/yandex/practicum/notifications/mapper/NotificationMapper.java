package ru.yandex.practicum.notifications.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;

import java.time.LocalDateTime;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationRequest request) {
        return Notification.builder()
                .login(request.getLogin())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
