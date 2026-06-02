package ru.yandex.practicum.notifications.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToController(notificationController)
                .build();
    }

    @Test
    void notificate_shouldReturnOk() {
        NotificationRequest request = NotificationRequest.builder()
                .login("test_user")
                .message("Test notification message")
                .build();

        when(notificationService.logNotification(any(NotificationRequest.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        verify(notificationService).logNotification(any(NotificationRequest.class));
    }

    @Test
    void notificate_whenLoginBlank_shouldReturnBadRequest() {
        NotificationRequest request = NotificationRequest.builder()
                .login("")
                .message("Test message")
                .build();

        webTestClient.post()
                .uri("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void notificate_whenMessageBlank_shouldReturnBadRequest() {
        NotificationRequest request = NotificationRequest.builder()
                .login("test_user")
                .message("")
                .build();

        webTestClient.post()
                .uri("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
