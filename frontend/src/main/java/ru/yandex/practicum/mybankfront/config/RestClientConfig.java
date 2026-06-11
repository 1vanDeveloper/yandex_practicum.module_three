package ru.yandex.practicum.mybankfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(OAuth2AuthorizedClientService authorizedClientService) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OAuth2ClientHttpRequestInterceptor(authorizedClientService));
        return restTemplate;
    }

    private static class OAuth2ClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
        private final OAuth2AuthorizedClientService authorizedClientService;

        public OAuth2ClientHttpRequestInterceptor(OAuth2AuthorizedClientService authorizedClientService) {
            this.authorizedClientService = authorizedClientService;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            // Get current authentication from SecurityContext
            OAuth2AuthenticationToken authentication = 
                (OAuth2AuthenticationToken) org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            
            if (authentication != null) {
                // Load authorized client
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    "frontend-service", authentication.getName());
                
                if (client != null && client.getAccessToken() != null) {
                    // Add bearer token to request
                    request.getHeaders().set("Authorization", 
                        "Bearer " + client.getAccessToken().getTokenValue());
                }
            }
            
            return execution.execute(request, body);
        }
    }
}
