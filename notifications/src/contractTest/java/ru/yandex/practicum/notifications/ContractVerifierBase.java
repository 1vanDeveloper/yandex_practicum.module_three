package ru.yandex.practicum.notifications;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.notifications.config.TestSecurityConfig;
import ru.yandex.practicum.notifications.service.NotificationService;

import java.util.concurrent.CompletableFuture;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0"
        })
@AutoConfigureMessageVerifier
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, ContractVerifierBase.MockServiceConfig.class})
public abstract class ContractVerifierBase {
    @LocalServerPort
    protected int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(IllegalArgumentException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        void handleBadRequest(IllegalArgumentException ex) {}
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class MockServiceConfig {
        @Bean
        public NotificationService notificationService() {
            NotificationService mock = Mockito.mock(NotificationService.class);
            Mockito.doNothing().when(mock).logNotification(Mockito.any());
            Mockito.doNothing().when(mock).saveNotification(Mockito.any());
            return mock;
        }

        @Bean
        public ru.yandex.practicum.notifications.service.KafkaNotificationConsumer kafkaNotificationConsumer() {
            return Mockito.mock(ru.yandex.practicum.notifications.service.KafkaNotificationConsumer.class);
        }
    }
}
