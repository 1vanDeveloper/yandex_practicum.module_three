package ru.yandex.practicum.cash.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8180/realms/bank/protocol/openid-connect/certs}")
    private String jwkSetUri;

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeLongEnough}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                        // All cash endpoints require authentication (called by gateway with OAuth2)
                        .requestMatchers("/cash/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(compositeJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtDecoder compositeJwtDecoder() {
        // Декодер для локальных JWT токенов (от Accounts сервиса, HMAC256)
        byte[] keyBytes = jwtSecret.getBytes();
        javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");
        NimbusJwtDecoder localJwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();

        // Декодер для JWT токенов от Keycloak (RS256)
        NimbusJwtDecoder keycloakJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Проверяем issuer токена для выбора правильного декодера
        return token -> {
            try {
                // Распарсиваем токен без проверки подписи для получения issuer
                String[] parts = token.split("\\.");
                if (parts.length != 3) {
                    throw new org.springframework.security.oauth2.jwt.BadJwtException("Invalid JWT format");
                }
                
                // Декодируем payload (вторая часть)
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
                String issuer = jsonNode.has("iss") ? jsonNode.get("iss").asText() : null;
                
                // Если токен от Keycloak — используем Keycloak декодер
                if (issuer != null && (issuer.contains("keycloak") || issuer.contains("localhost:8180") || issuer.contains("8080/realms"))) {
                    return keycloakJwtDecoder.decode(token);
                } else {
                    // Иначе — локальный декодер
                    return localJwtDecoder.decode(token);
                }
            } catch (Exception e) {
                // Если не удалось получить issuer, пробуем локальный декодер
                return localJwtDecoder.decode(token);
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            
            // Извлекаем привилегии из claim "privileges"
            List<String> privileges = jwt.getClaimAsStringList("privileges");
            if (privileges != null) {
                for (String privilege : privileges) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(privilege));
                }
            }
            
            // Также добавляем роли из realm_access.roles (для совместимости с Keycloak)
            try {
                java.util.Collection<GrantedAuthority> realmRoles = grantedAuthoritiesConverter.convert(jwt);
                if (realmRoles != null) {
                    authorities.addAll(realmRoles);
                }
            } catch (Exception e) {
                // Игнорируем, если realm_access.roles отсутствует
            }
            
            return authorities;
        });
        return jwtAuthenticationConverter;
    }
}
