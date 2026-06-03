package ru.yandex.practicum.accounts.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.Collections;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    @Profile("test")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    @Primary
    @Profile("test")
    public JwtDecoder jwtDecoder() {
        return token -> createTestJwt("test_user");
    }

    @Bean
    @Primary
    @Profile("test")
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt ->
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return converter;
    }

    private Jwt createTestJwt(String login) {
        return new Jwt(
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Collections.singletonMap("alg", "none"),
            Collections.singletonMap("preferred_username", login)
        );
    }
}
