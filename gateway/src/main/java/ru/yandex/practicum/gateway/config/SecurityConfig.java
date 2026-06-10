package ru.yandex.practicum.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/gateway/register").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.gateway_service.client-id")
    public InMemoryClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("gateway_service")
                .clientId(System.getenv().getOrDefault("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GATEWAY_SERVICE_CLIENT_ID", "gateway-client"))
                .clientSecret(System.getenv().getOrDefault("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GATEWAY_SERVICE_CLIENT_SECRET", "gateway-secret"))
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .tokenUri(System.getenv().getOrDefault("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_TOKEN_URI", "http://keycloak:8080/realms/bank/protocol/openid-connect/token"))
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
