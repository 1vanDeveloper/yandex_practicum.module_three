package ru.yandex.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.mapper.NotificationMapper;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public Mono<Void> logNotification(NotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);
        
        log.info("Notification logged for user {}: {}", request.getLogin(), request.getMessage());
        
        return notificationRepository.save(notification)
                .then();
    }
}
