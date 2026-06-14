package ru.yandex.practicum.accounts.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.List;

@TestConfiguration
@EnableWebSecurity
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
                .jwt(jwtConfig -> jwtConfig
                    .decoder(testJwtDecoder())
                    .jwtAuthenticationConverter(token -> 
                        new JwtAuthenticationToken(token, List.of(new SimpleGrantedAuthority("ROLE_USER")), token.getSubject())
                    )
                )
            );

        return http.build();
    }

    @Bean
    @Primary
    @Profile("test")
    public JwtDecoder testJwtDecoder() {
        return token -> {
            // Создаём mock JWT токен для тестов
            return Jwt.withTokenValue(token)
                    .subject("test_user")
                    .claim("privileges", List.of("accounts:read", "accounts:write"))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .header("alg", "HS256")
                    .build();
        };
    }

    @Bean
    @Primary
    @Profile("test")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    @Profile("test")
    public OAuth2AuthorizedClientManager authorizedClientManager() {
        return request -> {
            throw new UnsupportedOperationException("OAuth2 client not supported in tests");
        };
    }
}
