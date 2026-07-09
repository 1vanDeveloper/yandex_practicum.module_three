package ru.yandex.practicum.gateway.config;

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
 * Фильтр для propagation B3 заголовков трассировки в Spring Cloud Gateway.
 * Работает на уровне HTTP заголовков для реактивного стека.
 */
@Component
public class PropagationFilter implements GlobalFilter, Ordered {

    private static final String X_B3_TRACE_ID = "X-B3-TraceId";
    private static final String X_B3_SPAN_ID = "X-B3-SpanId";
    private static final String X_B3_SAMPLED = "X-B3-Sampled";

    private final Tracer tracer;
    private final Propagator propagator;

    public PropagationFilter(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Создаём span для этого запроса
        Span span = tracer.nextSpan()
            .name(exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI().getPath())
            .start();

        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            // Мутируем обмен, добавляя B3 заголовки в исходящий запрос
            ServerWebExchange mutated = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .headers(h -> {
                        // Inject B3 headers
                        propagator.inject(span.context(), h, (headers, key, value) -> {
                            if (value != null) {
                                headers.set(key, value);
                            }
                        });
                    })
                    .build())
                .build();

            return chain.filter(mutated).doFinally(signalType -> span.end());
        }
    }

    @Override
    public int getOrder() {
        // Выполняем после SecurityFilter, но до маршрутизации
        return -100;
    }
}
