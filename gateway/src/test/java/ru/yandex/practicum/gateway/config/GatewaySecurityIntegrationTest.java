package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration-тесты для Security конфигурации Gateway
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealthEndpointShouldBeAccessible() {
        // Act & Assert
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void actuatorInfoEndpointShouldBeAccessible() {
        // Act & Assert
        webTestClient.get()
            .uri("/actuator/info")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void protectedEndpointShouldRequireAuthentication() {
        // Act & Assert - запрос без токена должен вернуть 401
        webTestClient.get()
            .uri("/gateway/account")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void protectedCashEndpointShouldRequireAuthentication() {
        // Act & Assert
        webTestClient.post()
            .uri("/gateway/cash")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void protectedTransferEndpointShouldRequireAuthentication() {
        // Act & Assert
        webTestClient.post()
            .uri("/gateway/transfer")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void protectedAccountsListEndpointShouldRequireAuthentication() {
        // Act & Assert
        webTestClient.get()
            .uri("/gateway/accounts")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
