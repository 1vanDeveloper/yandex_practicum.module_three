package ru.yandex.practicum.accounts.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.yandex.practicum.accounts.filter.JwtAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeLongEnough}")
    private String jwtSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8180/realms/bank/protocol/openid-connect/certs}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - must be before oauth2ResourceServer
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                // Internal endpoints - require authentication (called by other services)
                .requestMatchers("/accounts/internal/**").authenticated()
                // Account access by login - require authentication
                .requestMatchers(HttpMethod.GET, "/accounts/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/accounts/**").authenticated()
                // All other requests - require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // OAuth2 Resource Server для валидации JWT от Keycloak (Client Credentials Flow)
            // Применяется только к запросам с Authorization header
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(compositeJwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            // JwtAuthenticationFilter для валидации локальных JWT токенов (пользовательская аутентификация)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public JwtDecoder compositeJwtDecoder() {
        // Создаём декодер для локальных JWT токенов (от Accounts сервиса)
        byte[] keyBytes = jwtSecret.getBytes();
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HMACSHA256");
        NimbusJwtDecoder localJwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();

        // Создаём декодер для JWT токенов от Keycloak
        NimbusJwtDecoder keycloakJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Возвращаем декодер, который пробует оба варианта
        return token -> {
            try {
                // Сначала пробуем валидировать как Keycloak токен
                return keycloakJwtDecoder.decode(token);
            } catch (Exception e) {
                // Если не получилось, пробуем локальный токен
                return localJwtDecoder.decode(token);
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            
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
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
