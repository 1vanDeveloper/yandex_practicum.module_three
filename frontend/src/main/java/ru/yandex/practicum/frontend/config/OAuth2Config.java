package ru.yandex.practicum.frontend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * OAuth2 конфигурация для аутентификации через Keycloak.
 * Включается только когда spring.security.enabled=true.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2Config {
    // Конфигурация OAuth2 задаётся через application.properties
}
