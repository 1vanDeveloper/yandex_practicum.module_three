package ru.yandex.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient конфигурация через Builder для поддержки трейсинга.
 * Spring Boot автоматически добавляет traceparent заголовки через Observation.
 */
@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        // RestClient.Builder автоматически поддерживает трейсинг через Observation
        return RestClient.builder();
    }
    
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
