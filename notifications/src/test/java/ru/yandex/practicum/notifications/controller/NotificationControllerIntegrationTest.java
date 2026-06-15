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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController.
 * Tests verify HTTP endpoint behavior with real Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class NotificationControllerIntegrationTest {

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
    @DisplayName("POST /notifications/notificate - успешное создание уведомления")
    void notificate_shouldReturnOkAndSaveNotification() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test notification message")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk());

        Thread.sleep(1000); // Wait for async completion
        
        assertEquals(1, notificationRepository.count());
    }

    @Test
    @DisplayName("POST /notifications/notificate - пустой login возвращает 400")
    void notificate_withEmptyLogin_shouldReturnBadRequest() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("")
            .message("Test message")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - отсутствующий login возвращает 400")
    void notificate_withMissingLogin_shouldReturnBadRequest() throws Exception {
        String requestBody = "{\"message\": \"Test message\"}";

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - пустое сообщение возвращает 400")
    void notificate_withEmptyMessage_shouldReturnBadRequest() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - отсутствующее сообщение возвращает 400")
    void notificate_withMissingMessage_shouldReturnBadRequest() throws Exception {
        String requestBody = "{\"login\": \"test_user\"}";

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - login короче 3 символов возвращает 400")
    void notificate_withShortLogin_shouldReturnBadRequest() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("ab")
            .message("Test message")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - сообщение длиннее 1000 символов возвращает 400")
    void notificate_withLongMessage_shouldReturnBadRequest() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("A".repeat(1001))
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - некорректный JSON возвращает 400")
    void notificate_withInvalidJson_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /notifications/notificate - корректный Content-Type")
    void notificate_withCorrectContentType_shouldProcess() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("test_user")
            .message("Test message")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /notifications/notificate - асинхронная обработка")
    void notificate_shouldProcessAsync() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
            .login("async_user")
            .message("Async test message")
            .build();

        mockMvc.perform(post("/notifications/notificate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk());

        Thread.sleep(1000); // Wait for async completion
        
        assertEquals(1, notificationRepository.count());
    }
}
