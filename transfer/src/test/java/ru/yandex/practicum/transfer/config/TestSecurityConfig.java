package ru.yandex.practicum.transfer.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.*;

/**
 * Test configuration for unit tests with disabled security.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(exchanges -> exchanges.anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.disable())
            .oauth2Client(oauth2 -> oauth2.disable())
            .build();
    }

    @Bean
    @Primary
    public OAuth2AuthorizedClientManager mockAuthorizedClientManager() {
        return mock(OAuth2AuthorizedClientManager.class);
    }
}
