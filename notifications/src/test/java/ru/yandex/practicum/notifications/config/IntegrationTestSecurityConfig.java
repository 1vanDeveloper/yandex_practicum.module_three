package ru.yandex.practicum.notifications.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration for integration tests with disabled security.
 * Use this profile when you want to skip JWT validation.
 * For tests with real Keycloak JWT validation, use the 'keycloak' profile instead.
 */
@TestConfiguration
@Profile("test")
public class IntegrationTestSecurityConfig {

    @Bean
    public SecurityFilterChain integrationTestSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(exchanges -> exchanges
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/notifications/**").permitAll()
                .anyRequest().permitAll()
            )
            .build();
    }
}
