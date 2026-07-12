package ru.yandex.practicum.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter для явной передачи B3 заголовков в downstream сервисы.
 * 
 * Spring Cloud Gateway 2024.0.0 не автоматически propagates B3 headers через WebClient,
 * поэтому добавляем их явно используя ServerHttpRequestDecorator.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class B3PropagationGlobalFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;
    private final Propagator propagator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Получаем текущий span из контекста
        Span currentSpan = tracer.currentSpan();
        
        if (currentSpan != null) {
            // Создаём mutable заголовки для injection
            HttpHeaders b3Headers = new HttpHeaders();
            
            // Inject B3 headers в HttpHeaders
            propagator.inject(currentSpan.context(), b3Headers, HttpHeaders::set);
            
            // Создаём декоратор запроса который добавит B3 заголовки
            ServerHttpRequest decoratedRequest = new B3HeaderServerHttpRequestDecorator(
                exchange.getRequest(), b3Headers);
            
            // Создаём новый exchange с декорированным запросом
            ServerWebExchange decoratedExchange = exchange.mutate()
                .request(decoratedRequest)
                .build();
            
            log.debug("B3 headers injected: traceId={}, spanId={}, headers={}", 
                currentSpan.context().traceId(), 
                currentSpan.context().spanId(),
                b3Headers);
            
            return chain.filter(decoratedExchange);
        } else {
            log.debug("No current span found for B3 propagation");
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Выполняем после LoadBalancerFilter но до NettyRoutingFilter
        // LoadBalancerFilter имеет order = LOAD_BALANCER_CLIENT_FILTER_ORDER (10150)
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 100;
    }
    
    /**
     * Декоратор ServerHttpRequest для добавления B3 заголовков
     */
    private static class B3HeaderServerHttpRequestDecorator extends ServerHttpRequestDecorator {
        
        private final HttpHeaders b3Headers;
        
        B3HeaderServerHttpRequestDecorator(ServerHttpRequest delegate, HttpHeaders b3Headers) {
            super(delegate);
            this.b3Headers = b3Headers;
        }
        
        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(super.getHeaders());
            headers.addAll(b3Headers);
            return headers;
        }
    }
}
