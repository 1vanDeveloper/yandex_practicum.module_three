package ru.yandex.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.mapper.NotificationMapper;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public CompletableFuture<Void> logNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Notification notification = notificationMapper.toEntity(request);

            log.info("Notification logged for user {}: {}", request.getLogin(), request.getMessage());

            notificationRepository.save(notification);
            return null;
        });
    }
}
