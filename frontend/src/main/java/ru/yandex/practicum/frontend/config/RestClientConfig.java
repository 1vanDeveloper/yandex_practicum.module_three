package ru.yandex.practicum.frontend.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * RestClient конфигурация с явной B3 propagation.
 * Извлекает B3 заголовки из входящего запроса и передаёт в исходящие.
 */
@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;

    @Bean
    public ClientHttpRequestInterceptor b3PropagationInterceptor() {
        return (request, body, execution) -> {
            // Получаем текущий HTTP запрос (входящий от браузера)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            String traceId = null;
            String newSpanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String incomingSampled = null;
            
            if (attributes != null) {
                HttpServletRequest servletRequest = attributes.getRequest();
                
                // Извлекаем B3 заголовки из входящего запроса
                String incomingTraceId = servletRequest.getHeader("X-B3-TraceId");
                String incomingSpanId = servletRequest.getHeader("X-B3-SpanId");
                incomingSampled = servletRequest.getHeader("X-B3-Sampled");
                
                // Используем входящий trace ID или генерируем новый
                traceId = (incomingTraceId != null && !incomingTraceId.isEmpty())
                    ? incomingTraceId
                    : UUID.randomUUID().toString().replace("-", "");
                
                // Обрезаем до 32 символов (128 бит)
                if (traceId.length() > 32) {
                    traceId = traceId.substring(0, 32);
                }
                
                log.debug("B3PropagationInterceptor: Incoming - TraceId={}, SpanId={}", incomingTraceId, incomingSpanId);
            } else {
                // Нет контекста запроса — генерируем новый trace ID
                traceId = UUID.randomUUID().toString().replace("-", "");
                log.debug("B3PropagationInterceptor: No request context, generated new TraceId={}", traceId);
            }
            
            log.debug("B3PropagationInterceptor: Outgoing - TraceId={}, SpanId={}", traceId, newSpanId);
            
            // Добавляем B3 заголовки к исходящему запросу
            HttpHeaders headers = request.getHeaders();
            headers.set("X-B3-TraceId", traceId);
            headers.set("X-B3-SpanId", newSpanId);
            if ("1".equals(incomingSampled)) {
                headers.set("X-B3-Sampled", "1");
            }
            
            return execution.execute(request, body);
        };
    }

    @Bean
    public RestClient.Builder restClientBuilder(ClientHttpRequestInterceptor b3PropagationInterceptor) {
        return RestClient.builder()
            .requestInterceptor(b3PropagationInterceptor);
    }
}
