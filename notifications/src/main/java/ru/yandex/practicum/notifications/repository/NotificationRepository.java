package ru.yandex.practicum.notifications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.notifications.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
