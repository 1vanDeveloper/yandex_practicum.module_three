package ru.yandex.practicum.frontend.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
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

    @Bean
    public ClientHttpRequestInterceptor tracingInterceptor(Tracer tracer, Propagator propagator) {
        return (request, body, execution) -> {
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
        };
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestInterceptor tracingInterceptor, ObservationRegistry observationRegistry) {
        return RestClient.builder()
            .requestInterceptor(tracingInterceptor)
            .observationRegistry(observationRegistry);
    }
}
