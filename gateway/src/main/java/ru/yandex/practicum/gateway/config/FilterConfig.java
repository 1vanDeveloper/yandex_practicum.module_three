package ru.yandex.practicum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.gateway.filter.JwtAuthFilter;

/**
 * Конфигурация для регистрации GlobalFilter.
 */
@Configuration
public class FilterConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter();
    }
}
