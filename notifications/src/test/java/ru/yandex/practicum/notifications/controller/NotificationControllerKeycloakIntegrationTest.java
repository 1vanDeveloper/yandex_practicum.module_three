package ru.yandex.practicum.notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.notifications.dto.NotificationRequest;
import ru.yandex.practicum.notifications.repository.NotificationRepository;
import ru.yandex.practicum.notifications.util.KeycloakTokenUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController with Keycloak JWT authentication.
 * 
 * Перед запуском убедитесь, что сервисы запущены:
 * docker-compose up -d keycloak postgres
 * 
 * Тесты используют реальные JWT токены от Keycloak (localhost:8180).
 * Запуск: ./gradlew :notifications:test --tests "*KeycloakIntegrationTest*" -Dspring.profiles.active=keycloak
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"integration", "keycloak"})
class NotificationControllerKeycloakIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NotificationRepository notificationRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /notifications/notificate - с валидным JWT токеном от Keycloak (user token)")
    void notificate_withValidUserJwtToken_shouldReturnOk() throws Exception {
        // Получаем токен пользователя от Keycloak (realm-export.json: user/password)
        String token = KeycloakTokenUtil.getUserToken("user", "password");

        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test with JWT authentication")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        assertEquals(1, notificationRepository.count());
    }

    @Test
    @DisplayName("POST /notifications/notificate - с service account токеном от Keycloak (accounts-client)")
    void notificate_withValidClientJwtToken_shouldReturnOk() throws Exception {
        // Получаем service account токен от Keycloak для accounts-client
        // accounts-client имеет роль notifications:send по умолчанию
        String token = KeycloakTokenUtil.getClientToken("accounts-client", "accounts-secret");

        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test with client credentials")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        assertEquals(1, notificationRepository.count());
    }
}
