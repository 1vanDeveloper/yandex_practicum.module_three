package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.event.NotificationEvent;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationService using PostgreSQL from Kubernetes.
 * Перед запуском убедитесь, что настроен port-forward:
 *   kubectl port-forward svc/postgresql 5432:5432 &
 * Tests verify database interactions with real PostgreSQL instance from Kubernetes cluster.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Сохранение уведомления из Kafka события в БД")
    void saveNotification_shouldSaveNotificationToDatabase() {
        NotificationEvent event = createTestEvent("test_user", "Test notification message");

        notificationService.saveNotification(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        
        Notification saved = notifications.getFirst();
        assertEquals("test_user", saved.getLogin());
        assertEquals("Test notification message", saved.getMessage());
        assertNotNull(saved.getCreatedAt());
        assertTrue(LocalDateTime.now().isAfter(saved.getCreatedAt().minusSeconds(1)));
    }

    @Test
    @DisplayName("Сохранение нескольких уведомлений для разных пользователей")
    void saveNotification_shouldSaveMultipleNotifications() {
        NotificationEvent event1 = createTestEvent("user1", "Message for user 1");
        NotificationEvent event2 = createTestEvent("user2", "Message for user 2");

        notificationService.saveNotification(event1);
        notificationService.saveNotification(event2);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(2, notifications.size());
        
        List<String> logins = notifications.stream()
            .map(Notification::getLogin)
            .toList();
        
        assertTrue(logins.contains("user1"));
        assertTrue(logins.contains("user2"));
    }

    @Test
    @DisplayName("Сохранение уведомления с длинным сообщением")
    void saveNotification_shouldSaveNotificationWithLongMessage() {
        String longMessage = "A".repeat(1000);
        NotificationEvent event = createTestEvent("test_user", longMessage);

        notificationService.saveNotification(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(longMessage, notifications.getFirst().getMessage());
    }

    @Test
    @DisplayName("Сохранение уведомления с специальными символами в сообщении")
    void saveNotification_shouldSaveNotificationWithSpecialCharacters() {
        String specialMessage = "Test with special chars: äöü ñ Привет 🚀";
        NotificationEvent event = createTestEvent("test_user", specialMessage);

        notificationService.saveNotification(event);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(specialMessage, notifications.getFirst().getMessage());
    }

    @Test
    @DisplayName("Временная метка устанавливается при сохранении")
    void saveNotification_shouldSetCreatedAtTimestamp() {
        LocalDateTime before = LocalDateTime.now();
        NotificationEvent event = createTestEvent("test_user", "Test message");

        notificationService.saveNotification(event);

        LocalDateTime after = LocalDateTime.now();
        
        Notification saved = notificationRepository.findAll().getFirst();
        assertTrue(saved.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(saved.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    private NotificationEvent createTestEvent(String login, String message) {
        return NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .accountId(login)
                .login(login)
                .message(message)
                .type("TEST_EVENT")
                .timestamp(Instant.now())
                .build();
    }
}
