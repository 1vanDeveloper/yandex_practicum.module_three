package ru.yandex.practicum.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.gateway-service.client-id", matchIfMissing = false)
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        return new org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
        );
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.gateway-service.client-id", matchIfMissing = false)
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return WebClient.builder()
                .filter(oauth2ClientCredentialsFilter(authorizedClientManager, "gateway-service"))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.gateway-service.client-id", matchIfMissing = true, havingValue = "false")
    public WebClient defaultWebClient() {
        return WebClient.builder().build();
    }

    private ExchangeFilterFunction oauth2ClientCredentialsFilter(
            OAuth2AuthorizedClientManager authorizedClientManager,
            String clientRegistrationId) {
        return (request, next) -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(clientRegistrationId)
                    .principal("gateway-service")
                    .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            ClientRequest mutatedRequest = request;
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                mutatedRequest = ClientRequest.from(request)
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                        .build();
            }
            return next.exchange(mutatedRequest);
        };
    }
}
