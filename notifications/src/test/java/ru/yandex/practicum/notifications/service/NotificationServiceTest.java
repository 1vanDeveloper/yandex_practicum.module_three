package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.event.NotificationEvent;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationEvent event;
    private Notification notification;

    @BeforeEach
    void setUp() {
        event = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId("test_user")
                .login("test_user")
                .message("Test notification message")
                .type("TEST_EVENT")
                .timestamp(Instant.now())
                .build();

        notification = Notification.builder()
                .id(1L)
                .login("test_user")
                .message("Test notification message")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void saveNotification_shouldSaveAndLog() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.saveNotification(event);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void saveNotification_shouldMapEventToEntity() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.saveNotification(event);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();

        assertThat(capturedNotification.getLogin()).isEqualTo("test_user");
        assertThat(capturedNotification.getMessage()).isEqualTo("Test notification message");
    }

    @Test
    void saveNotification_whenExceptionThrown_shouldCompleteExceptionally() {
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> notificationService.saveNotification(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }
}
