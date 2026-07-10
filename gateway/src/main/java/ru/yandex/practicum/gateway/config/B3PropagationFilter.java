package ru.yandex.practicum.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GlobalFilter для передачи B3 заголовков через Spring Cloud Gateway.
 * Вручную копирует входящие B3 заголовки в исходящий запрос.
 */
@Component
public class B3PropagationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(B3PropagationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Пропускаем health check и статические ресурсы
        if (path.contains("/actuator/") || path.equals("/")) {
            return chain.filter(exchange);
        }

        // Получаем входящие B3 заголовки
        String incomingTraceId = request.getHeaders().getFirst("X-B3-TraceId");
        String incomingSpanId = request.getHeaders().getFirst("X-B3-SpanId");
        String incomingSampled = request.getHeaders().getFirst("X-B3-Sampled");

        // Генерируем новый span ID для gateway
        String gatewaySpanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Используем входящий trace ID или генерируем новый
        String traceId = (incomingTraceId != null && !incomingTraceId.isEmpty()) 
            ? incomingTraceId 
            : UUID.randomUUID().toString().replace("-", "");

        // Обрезаем traceId до 32 символов (128 бит)
        if (traceId.length() > 32) {
            traceId = traceId.substring(0, 32);
        }

        final String finalTraceId = traceId;
        final String finalGatewaySpanId = gatewaySpanId;
        final String finalIncomingSampled = incomingSampled;

        log.debug("B3PropagationFilter: Incoming - TraceId={}, SpanId={}, Sampled={}", 
            incomingTraceId, incomingSpanId, incomingSampled);
        log.debug("B3PropagationFilter: Outgoing - TraceId={}, SpanId={}", traceId, gatewaySpanId);

        // Создаём исходящие B3 заголовки
        ServerHttpRequest mutatedRequest = request.mutate()
            .headers(headers -> {
                // Удаляем старые B3 заголовки
                headers.remove("X-B3-TraceId");
                headers.remove("X-B3-SpanId");
                headers.remove("X-B3-Sampled");
                headers.remove("b3");
                
                // Добавляем новые заголовки
                headers.set("X-B3-TraceId", finalTraceId);
                headers.set("X-B3-SpanId", finalGatewaySpanId);
                if ("1".equals(finalIncomingSampled)) {
                    headers.set("X-B3-Sampled", "1");
                }
            })
            .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
