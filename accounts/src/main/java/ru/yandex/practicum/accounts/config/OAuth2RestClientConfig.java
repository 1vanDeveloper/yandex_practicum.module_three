package ru.yandex.practicum.accounts.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Configuration
public class OAuth2RestClientConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.accounts-service.client-id", matchIfMissing = false)
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        return new org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
        );
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.accounts-service.client-id", matchIfMissing = false)
    public RestClient restClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        RestTemplate restTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(new OAuth2ClientCredentialsInterceptor(authorizedClientManager, "accounts-service"));
        restTemplate.setInterceptors(interceptors);

        return RestClient.builder()
                .requestFactory(restTemplate.getRequestFactory())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.security.oauth2.client.registration.accounts-service.client-id", matchIfMissing = true, havingValue = "false")
    public RestClient defaultRestClient() {
        return RestClient.create();
    }

    private static class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {
        private final OAuth2AuthorizedClientManager authorizedClientManager;
        private final String clientRegistrationId;

        OAuth2ClientCredentialsInterceptor(OAuth2AuthorizedClientManager authorizedClientManager, String clientRegistrationId) {
            this.authorizedClientManager = authorizedClientManager;
            this.clientRegistrationId = clientRegistrationId;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(clientRegistrationId)
                    .principal("accounts-service")
                    .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authRequest);
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                request.getHeaders().set(
                        org.springframework.http.HttpHeaders.AUTHORIZATION,
                        "Bearer " + authorizedClient.getAccessToken().getTokenValue()
                );
            }
            return execution.execute(request, body);
        }
    }
}
