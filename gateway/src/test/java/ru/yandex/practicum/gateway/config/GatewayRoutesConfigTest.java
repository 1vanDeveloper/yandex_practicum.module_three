package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void routeLocatorShouldBeCreated() {
        assertNotNull(routeLocator, "RouteLocator должен быть создан");
    }
}
