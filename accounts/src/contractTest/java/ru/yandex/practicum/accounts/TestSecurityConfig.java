package ru.yandex.practicum.accounts;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtSpec -> jwtSpec
                    .jwtDecoder(testJwtDecoder())
                    .jwtAuthenticationConverter(token ->
                        Mono.just(new JwtAuthenticationToken(
                            createTestJwt(),
                            AuthorityUtils.createAuthorityList("ROLE_USER"),
                            "test_user"
                        ))
                    )
                )
            )
            .build();
    }

    @Bean
    @Primary
    public ReactiveJwtDecoder testJwtDecoder() {
        return token -> Mono.just(createTestJwt());
    }
    
    private Jwt createTestJwt() {
        return Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .header("typ", "JWT")
            .claim("preferred_username", "test_user")
            .claim("sub", "test_user")
            .claim("iat", Instant.now().getEpochSecond())
            .subject("test_user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}
