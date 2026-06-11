package ru.yandex.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                // Registration endpoint - public (called during user registration)
                .requestMatchers(HttpMethod.POST, "/accounts").permitAll()
                // Internal endpoints - require authentication (called by other services)
                .requestMatchers("/accounts/internal/**").authenticated()
                // Account access by login - require authentication (must come after /accounts/internal/**)
                .requestMatchers(HttpMethod.GET, "/accounts/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/accounts/*").authenticated()
                // All other requests - require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

        return http.build();
    }
}
