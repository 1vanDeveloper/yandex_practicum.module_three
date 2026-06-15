package ru.yandex.practicum.notifications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.entity.Notification;
import ru.yandex.practicum.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationService using PostgreSQL and Keycloak from docker-compose.
 * 
 * Перед запуском убедитесь, что сервисы запущены:
 * docker-compose up -d postgres keycloak
 * 
 * Tests verify database interactions with real PostgreSQL instance from docker-compose.
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
    @DisplayName("Сохранение уведомления в БД")
    void logNotification_shouldSaveNotificationToDatabase() throws ExecutionException, InterruptedException {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test notification message")
            .build();

        notificationService.logNotification(request).get();

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        
        Notification saved = notifications.get(0);
        assertEquals("test_user", saved.getLogin());
        assertEquals("Test notification message", saved.getMessage());
        assertNotNull(saved.getCreatedAt());
        assertTrue(LocalDateTime.now().isAfter(saved.getCreatedAt().minusSeconds(1)));
    }

    @Test
    @DisplayName("Сохранение нескольких уведомлений для разных пользователей")
    void logNotification_shouldSaveMultipleNotifications() throws ExecutionException, InterruptedException {
        NotificationRequest request1 = NotificationRequest.builder()
            .login("user1")
            .message("Message for user 1")
            .build();

        NotificationRequest request2 = NotificationRequest.builder()
            .login("user2")
            .message("Message for user 2")
            .build();

        notificationService.logNotification(request1).get();
        notificationService.logNotification(request2).get();

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
    void logNotification_shouldSaveNotificationWithLongMessage() throws ExecutionException, InterruptedException {
        String longMessage = "A".repeat(1000);
        
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message(longMessage)
            .build();

        notificationService.logNotification(request).get();

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(longMessage, notifications.get(0).getMessage());
    }

    @Test
    @DisplayName("ID уведомления генерируется автоматически")
    void logNotification_shouldGenerateIdAutomatically() throws ExecutionException, InterruptedException {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test message")
            .build();

        notificationService.logNotification(request).get();

        Optional<Notification> saved = notificationRepository.findAll().stream().findFirst();
        assertTrue(saved.isPresent());
        assertNotNull(saved.get().getId());
        assertTrue(saved.get().getId() > 0);
    }

    @Test
    @DisplayName("Сохранение уведомления с специальными символами в сообщении")
    void logNotification_shouldSaveNotificationWithSpecialCharacters() throws ExecutionException, InterruptedException {
        String specialMessage = "Test with special chars: äöü ñ 中文 🚀";
        
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message(specialMessage)
            .build();

        notificationService.logNotification(request).get();

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(specialMessage, notifications.get(0).getMessage());
    }

    @Test
    @DisplayName("Временная метка устанавливается при сохранении")
    void logNotification_shouldSetCreatedAtTimestamp() throws ExecutionException, InterruptedException {
        LocalDateTime before = LocalDateTime.now();
        
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test message")
            .build();

        notificationService.logNotification(request).get();

        LocalDateTime after = LocalDateTime.now();
        
        Notification saved = notificationRepository.findAll().get(0);
        assertTrue(saved.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(saved.getCreatedAt().isBefore(after.plusSeconds(1)));
    }
}
