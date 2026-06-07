package ru.yandex.practicum.cash.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.time.Instant;

import static org.mockito.Mockito.*;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth2 -> oauth2.disable())
                .oauth2Client(oauth2 -> oauth2.disable());

        return http.build();
    }

    @Bean
    @Primary
    public OAuth2AuthorizedClientManager authorizedClientManager() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        
        when(manager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);
        
        return manager;
    }

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Create a dummy client registration to satisfy InMemoryClientRegistrationRepository
        ClientRegistration dummyClient = ClientRegistration
                .withRegistrationId("cash-service")
                .clientId("cash-client")
                .clientSecret("cash-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8080/token")
                .build();
        
        return new InMemoryClientRegistrationRepository(dummyClient);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
