package ru.yandex.practicum.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Map;

@Configuration
@Slf4j
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(ClientRegistrationRepository clientRegistrationRepository) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OAuth2ClientCredentialsInterceptor(clientRegistrationRepository, "gateway-service"));
        return restTemplate;
    }

    private static class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {
        private final ClientRegistrationRepository clientRegistrationRepository;
        private final String clientRegistrationId;
        private volatile String cachedToken;
        private volatile long tokenExpiresAt;

        public OAuth2ClientCredentialsInterceptor(ClientRegistrationRepository clientRegistrationRepository, String clientRegistrationId) {
            this.clientRegistrationRepository = clientRegistrationRepository;
            this.clientRegistrationId = clientRegistrationId;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            log.debug("Intercepting request to: {}", request.getURI());
            log.debug("Cached token null or expired: {}", cachedToken == null || System.currentTimeMillis() >= tokenExpiresAt);
            
            if (cachedToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
                synchronized (this) {
                    if (cachedToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
                        acquireToken();
                    }
                }
            }
            
            log.debug("Token acquired: {}", cachedToken != null);
            if (cachedToken != null) {
                request.getHeaders().setBearerAuth(cachedToken);
                log.debug("Bearer token added to request");
            } else {
                log.warn("No OAuth2 token available for request");
            }
            
            return execution.execute(request, body);
        }

        @SuppressWarnings("unchecked")
        private void acquireToken() {
            log.info("Acquiring OAuth2 token for registration: {}", clientRegistrationId);
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
            if (registration == null) {
                throw new IllegalStateException("Client registration not found: " + clientRegistrationId);
            }
            log.info("Found registration: clientId={}, tokenUri={}", registration.getClientId(), registration.getProviderDetails().getTokenUri());

            RestTemplate tokenRestTemplate = new RestTemplate();
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add(OAuth2ParameterNames.GRANT_TYPE, registration.getAuthorizationGrantType().getValue());
            formData.add(OAuth2ParameterNames.CLIENT_ID, registration.getClientId());
            formData.add(OAuth2ParameterNames.CLIENT_SECRET, registration.getClientSecret());

            log.info("Requesting token from: {}", registration.getProviderDetails().getTokenUri());
            Map<String, Object> tokenResponse = tokenRestTemplate.postForObject(
                    registration.getProviderDetails().getTokenUri(),
                    formData,
                    Map.class
            );

            log.info("Token response: {}", tokenResponse != null ? "received" : "null");
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to obtain access token. Response: {}", tokenResponse);
                throw new IllegalStateException("Failed to obtain access token");
            }

            cachedToken = (String) tokenResponse.get("access_token");
            tokenExpiresAt = System.currentTimeMillis() + 300000L; // 5 minutes
            log.info("OAuth2 token acquired successfully");
        }
    }
}
