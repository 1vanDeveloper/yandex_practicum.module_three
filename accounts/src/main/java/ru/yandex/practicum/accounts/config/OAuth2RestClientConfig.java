package ru.yandex.practicum.accounts.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2RestClientConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );
        manager.setAuthorizedClientProvider(provider);

        return manager;
    }

    @Bean
    public RestClient restClient(Tracer tracer, Propagator propagator) {
        return RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                Span parentSpan = tracer.currentSpan();
                if (parentSpan != null) {
                    Span span = tracer.nextSpan(parentSpan).name(request.getMethod() + " " + request.getURI().getPath()).start();
                    try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
                        HttpHeaders headers = request.getHeaders();
                        propagator.inject(span.context(), headers, (h, k, v) -> h.set(k, v));
                        return execution.execute(request, body);
                    } finally {
                        span.end();
                    }
                }
                return execution.execute(request, body);
            })
            .build();
    }
}
