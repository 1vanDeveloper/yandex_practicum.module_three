package ru.yandex.practicum.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${keycloak.admin-server-url:http://keycloak:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.target-realm:bank}")
    private String targetRealm;

    @Value("${spring.security.oauth2.client.registration.gateway_service.client-id:gateway-client}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.gateway_service.client-secret:gateway-secret}")
    private String clientSecret;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OAuth2ClientCredentialsInterceptor());
        return restTemplate;
    }

    private class OAuth2ClientCredentialsInterceptor implements ClientHttpRequestInterceptor {
        private volatile String cachedToken;
        private volatile long tokenExpiresAt;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            log.debug("Intercepting request to: {}", request.getURI());

            if (cachedToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
                synchronized (this) {
                    if (cachedToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
                        acquireToken();
                    }
                }
            }

            log.debug("Token available: {}", cachedToken != null);
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
            log.info("Acquiring OAuth2 token for client: {}", clientId);
            String tokenUri = keycloakServerUrl + "/realms/" + targetRealm + "/protocol/openid-connect/token";

            RestTemplate tokenRestTemplate = new RestTemplate();
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);

            log.info("Requesting token from: {}", tokenUri);
            Map<String, Object> tokenResponse = tokenRestTemplate.postForObject(
                    tokenUri,
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
