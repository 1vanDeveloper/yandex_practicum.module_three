package ru.yandex.practicum.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Глобальный фильтр для проброса JWT-токена пользователя в микросервисы.
 *
 * Извлекает JWT из SecurityContext и добавляет его в заголовок Authorization
 * для всех запросов к downstream-сервисам.
 */
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> {
                String token = null;
                if (context.getAuthentication() != null &&
                    context.getAuthentication().getPrincipal() instanceof Jwt jwt) {
                    token = jwt.getTokenValue();
                }
                
                var request = exchange.getRequest();
                
                // Добавляем JWT в заголовок Authorization для downstream-сервисов
                if (token != null) {
                    log.debug("Propagating JWT token to downstream service for path: {}", request.getPath());
                    var mutatedRequest = request.mutate()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                } else {
                    log.debug("No JWT token found in SecurityContext for path: {}", request.getPath());
                    return chain.filter(exchange);
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("SecurityContext is empty, continuing without JWT for path: {}", exchange.getRequest().getPath());
                return chain.filter(exchange);
            }));
    }

    @Override
    public int getOrder() {
        // Фильтр должен работать после аутентификации
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
