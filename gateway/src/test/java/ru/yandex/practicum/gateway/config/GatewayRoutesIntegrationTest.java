package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration-тесты для конфигурации маршрутов Gateway
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewayRoutesIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void accountsRouteShouldBeConfigured() {
        webTestClient.get()
            .uri("/gateway/accounts")
            .exchange()
            .expectStatus()
            .value(statusCode -> assertNotEquals(404, statusCode, "Маршрут accounts не должен возвращать 404"));
    }

    @Test
    void cashRouteShouldBeConfigured() {
        webTestClient.get()
            .uri("/gateway/cash")
            .exchange()
            .expectStatus()
            .value(statusCode -> assertNotEquals(404, statusCode, "Маршрут cash не должен возвращать 404"));
    }

    @Test
    void transferRouteShouldBeConfigured() {
        webTestClient.get()
            .uri("/gateway/transfer")
            .exchange()
            .expectStatus()
            .value(statusCode -> assertNotEquals(404, statusCode, "Маршрут transfer не должен возвращать 404"));
    }
}
