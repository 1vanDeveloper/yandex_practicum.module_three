package ru.yandex.practicum.accounts.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Profile("test")
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http, ReactiveJwtAuthenticationConverter converter) throws Exception {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(testJwtDecoder())
                    .jwtAuthenticationConverter(converter)
                )
            );

        return http.build();
    }

    @Bean
    @Primary
    @Profile("test")
    public ReactiveJwtDecoder testJwtDecoder() {
        return token -> Mono.just(createTestJwt("test_user"));
    }

    @Bean
    @Primary
    @Profile("test")
    public ReactiveJwtAuthenticationConverter testJwtAuthenticationConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> 
            Flux.just(new SimpleGrantedAuthority("ROLE_USER"))
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
