package ru.yandex.practicum.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация маршрутизации Spring Cloud Gateway.
 *
 * Маршруты:
 * - GET/PUT /gateway/account → accounts-service:/accounts (требует аутентификации)
 * - POST /gateway/cash → cash-service:/cash (требует аутентификации)
 * - POST /gateway/transfer → transfer-service:/transfer (требует аутентификации)
 *
 * Примечание: /gateway/login и /gateway/register больше не используются,
 * так как аутентификация осуществляется через Keycloak (OAuth2 Authorization Code Flow).
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // Accounts service routes - доступ к аккаунту пользователя
            .route("accounts-account-get", r -> r
                .path("/gateway/account")
                .and()
                .method(org.springframework.http.HttpMethod.GET)
                .filters(f -> f.rewritePath("/gateway/account", "/accounts/internal/me"))
                .uri("http://accounts:8080"))

            .route("accounts-account-update", r -> r
                .path("/gateway/account")
                .and()
                .method(org.springframework.http.HttpMethod.PUT)
                .filters(f -> f.rewritePath("/gateway/account", "/accounts/internal/me"))
                .uri("http://accounts:8080"))

            // Cash service routes
            .route("cash", r -> r
                .path("/gateway/cash")
                .filters(f -> f.rewritePath("/gateway/cash", "/cash"))
                .uri("http://cash:8080"))

            // Transfer service routes
            .route("transfer", r -> r
                .path("/gateway/transfer")
                .filters(f -> f.rewritePath("/gateway/transfer", "/transfer"))
                .uri("http://transfer:8080"))

            .build();
    }
}
