package ru.yandex.practicum.notifications.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.notifications.entity.Notification;

@Repository
public interface NotificationRepository extends R2dbcRepository<Notification, Long> {

    Mono<Notification> save(Notification notification);
}
