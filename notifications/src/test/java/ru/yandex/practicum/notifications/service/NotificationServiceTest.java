package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.mapper.NotificationMapper;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<NotificationRequest> requestCaptor;

    private NotificationRequest request;
    private Notification notification;

    @BeforeEach
    void setUp() {
        request = NotificationRequest.builder()
                .login("test_user")
                .message("Test notification message")
                .build();

        notification = Notification.builder()
                .id(1L)
                .login("test_user")
                .message("Test notification message")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void logNotification_shouldSaveAndLog() throws Exception {
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        CompletableFuture<Void> result = notificationService.logNotification(request);

        result.get();

        verify(notificationMapper).toEntity(any(NotificationRequest.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void logNotification_shouldMapRequestToEntity() throws Exception {
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.logNotification(request).get();

        verify(notificationMapper).toEntity(requestCaptor.capture());
        NotificationRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.getLogin()).isEqualTo("test_user");
        assertThat(capturedRequest.getMessage()).isEqualTo("Test notification message");
    }

    @Test
    void logNotification_whenExceptionThrown_shouldCompleteExceptionally() {
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("Database error"));

        CompletableFuture<Void> result = notificationService.logNotification(request);

        assertThatThrownBy(() -> result.get())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }
}
