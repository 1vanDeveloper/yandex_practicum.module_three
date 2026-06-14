package ru.yandex.practicum.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-тесты для конфигурации маршрутов Gateway
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewayRoutesIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void routeLocatorShouldBeCreated() {
        assertNotNull(routeLocator, "RouteLocator должен быть создан");
    }

    @Test
    void routesShouldBeRegistered() {
        // Проверяем, что маршруты зарегистрированы
        var routes = routeLocator.getRoutes().collectList().block();
        
        assertNotNull(routes, "Список маршрутов не должен быть null");
        assertTrue(routes.size() > 0, "Должен быть хотя бы один маршрут");
    }

    @Test
    void authLoginRouteShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        boolean hasAuthLoginRoute = routes.stream()
            .anyMatch(route -> "accounts-auth-login".equals(route.getId()));
        
        assertTrue(hasAuthLoginRoute, "Маршрут accounts-auth-login должен быть настроен");
    }

    @Test
    void authRegisterRouteShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        boolean hasAuthRegisterRoute = routes.stream()
            .anyMatch(route -> "accounts-auth-register".equals(route.getId()));
        
        assertTrue(hasAuthRegisterRoute, "Маршрут accounts-auth-register должен быть настроен");
    }

    @Test
    void accountRouteShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        boolean hasAccountRoute = routes.stream()
            .anyMatch(route -> "accounts-account-get".equals(route.getId()) || 
                              "accounts-account-update".equals(route.getId()));
        
        assertTrue(hasAccountRoute, "Маршрут accounts-account должен быть настроен");
    }

    @Test
    void cashRouteShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        boolean hasCashRoute = routes.stream()
            .anyMatch(route -> "cash".equals(route.getId()));
        
        assertTrue(hasCashRoute, "Маршрут cash должен быть настроен");
    }

    @Test
    void transferRouteShouldBeConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        
        boolean hasTransferRoute = routes.stream()
            .anyMatch(route -> "transfer".equals(route.getId()));
        
        assertTrue(hasTransferRoute, "Маршрут transfer должен быть настроен");
    }
}
