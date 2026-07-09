package ru.yandex.practicum.frontend.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * RestClient конфигурация с явной B3 propagation.
 */
@Configuration
public class RestClientConfig {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    @Bean
    public ClientHttpRequestInterceptor tracingInterceptor() {
        return (request, body, execution) -> {
            if (tracer != null && propagator != null) {
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
            }
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestInterceptor tracingInterceptor, ObservationRegistry observationRegistry) {
        return RestClient.builder()
            .requestInterceptor(tracingInterceptor)
            .observationRegistry(observationRegistry);
    }
}
