package ru.yandex.practicum.gateway.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Конфигурация OAuth2 Client для Client Credentials Flow.
 * Используется для получения сервисных токенов для межсервисных вызовов.
 */
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.client.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2ClientConfig {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                if (tracer != null && propagator != null) {
                    Span span = tracer.currentSpan();
                    if (span != null) {
                        HttpHeaders headers = new HttpHeaders();
                        propagator.inject(span.context(), headers, (h, k, v) -> h.set(k, v));
                        ClientRequest newRequest = ClientRequest.from(clientRequest)
                            .headers(h -> h.addAll(headers))
                            .build();
                        return Mono.just(newRequest);
                    }
                }
                return Mono.just(clientRequest);
            }));
    }
}
