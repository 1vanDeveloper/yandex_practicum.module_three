package ru.yandex.practicum.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация OAuth2 Client для Client Credentials Flow.
 * Используется для получения сервисных токенов для межсервисных вызовов.
 */
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2ClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
