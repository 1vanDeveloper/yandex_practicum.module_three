package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.mapper.NotificationMapper;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    void logNotification_shouldSaveAndLog() {
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(Mono.just(notification));

        StepVerifier.create(notificationService.logNotification(request))
                .verifyComplete();

        verify(notificationMapper).toEntity(any(NotificationRequest.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void logNotification_shouldMapRequestToEntity() {
        when(notificationMapper.toEntity(any(NotificationRequest.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(Mono.just(notification));

        notificationService.logNotification(request).block();

        verify(notificationMapper).toEntity(requestCaptor.capture());
        NotificationRequest capturedRequest = requestCaptor.getValue();

        assertThat(capturedRequest.getLogin()).isEqualTo("test_user");
        assertThat(capturedRequest.getMessage()).isEqualTo("Test notification message");
    }
}
