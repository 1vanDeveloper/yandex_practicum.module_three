package ru.yandex.practicum.transfer.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestInterceptor((ClientHttpRequestInterceptor) (request, body, execution) -> {
                if (tracer != null && propagator != null) {
                    Span span = tracer.currentSpan();
                    if (span != null) {
                        HttpHeaders headers = request.getHeaders();
                        propagator.inject(span.context(), headers, (h, k, v) -> h.set(k, v));
                    }
                }
                return execution.execute(request, body);
            });
    }
}
