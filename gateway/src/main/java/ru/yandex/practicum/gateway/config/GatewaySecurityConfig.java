package ru.yandex.practicum.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeLongEnough}")
    private String jwtSecret;

    @Bean
    @Primary
    public ReactiveJwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtSecret.getBytes();
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                // Public endpoints for user authentication
                .pathMatchers("/gateway/auth/login", "/gateway/auth/register").permitAll()
                .pathMatchers("/login/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtSpec -> jwtSpec
                    .jwtDecoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtToken -> {
                        List<GrantedAuthority> authorities = new ArrayList<>();
                        
                        // Извлекаем привилегии из claim "privileges"
                        List<String> privileges = jwtToken.getClaimAsStringList("privileges");
                        if (privileges != null) {
                            for (String privilege : privileges) {
                                authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(privilege));
                            }
                        }
                        
                        // Создаём Authentication с извлечёнными привилегиями
                        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authToken =
                            new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                                jwtToken,
                                authorities,
                                jwtToken.getSubject()
                            );
                        return reactor.core.publisher.Mono.just(authToken);
                    })
                )
            );

        return http.build();
    }
}
