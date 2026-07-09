package ru.yandex.practicum.gateway.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Фильтр для propagation B3 заголовков через Reactor Netty.
 * Явно добавляет B3 заголовки в каждый исходящий запрос.
 */
@Component
public class NettyTracingFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;
    private final Propagator propagator;

    public NettyTracingFilter(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Создаём span для входящего запроса
        Span span = tracer.nextSpan()
            .name(exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI().getPath())
            .start();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            // Inject B3 headers в текущий запрос
            HttpHeaders headers = new HttpHeaders();
            propagator.inject(span.context(), headers, (h, key, value) -> {
                if (value != null) {
                    h.set(key, value);
                }
            });

            // Добавляем B3 заголовки в атрибуты запроса для использования HttpClient
            ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .headers(h -> {
                        h.addAll(headers);
                    })
                    .build())
                .build();

            return chain.filter(mutatedExchange)
                .doFinally(signalType -> span.end());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
