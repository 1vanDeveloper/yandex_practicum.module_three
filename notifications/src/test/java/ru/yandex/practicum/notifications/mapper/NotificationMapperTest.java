package ru.yandex.practicum.notifications.mapper;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper notificationMapper = new NotificationMapper();

    @Test
    void toEntity_shouldMapRequestToEntity() {
        NotificationRequest request = NotificationRequest.builder()
                .login("test_user")
                .message("Test notification message")
                .build();

        Notification notification = notificationMapper.toEntity(request);

        assertThat(notification).isNotNull();
        assertThat(notification.getLogin()).isEqualTo("test_user");
        assertThat(notification.getMessage()).isEqualTo("Test notification message");
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getCreatedAt()).isAfterOrEqualTo(LocalDateTime.now().minusSeconds(1));
    }

    @Test
    void toEntity_shouldSetCurrentTimestamp() {
        NotificationRequest request = NotificationRequest.builder()
                .login("test_user")
                .message("Test message")
                .build();

        LocalDateTime before = LocalDateTime.now();
        Notification notification = notificationMapper.toEntity(request);
        LocalDateTime after = LocalDateTime.now();

        assertThat(notification.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(notification.getCreatedAt()).isBeforeOrEqualTo(after);
    }
}
