package ru.yandex.practicum.notifications.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * Фильтр для извлечения B3 заголовков из входящих HTTP запросов.
 */
@Configuration
public class ObservationConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservationConfig.class);

    private final Tracer tracer;
    private final Propagator propagator;
    private final ObservationRegistry observationRegistry;

    public ObservationConfig(Tracer tracer, Propagator propagator, ObservationRegistry observationRegistry) {
        this.tracer = tracer;
        this.propagator = propagator;
        this.observationRegistry = observationRegistry;
    }

    @Bean
    public FilterRegistrationBean<Filter> tracingFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TracingFilter(tracer, propagator));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("tracingFilter");
        log.info("TracingFilter registered with order {}", Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    public static class TracingFilter implements Filter {
        private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

        private final Tracer tracer;
        private final Propagator propagator;

        public TracingFilter(Tracer tracer, Propagator propagator) {
            this.tracer = tracer;
            this.propagator = propagator;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            log.info("TracingFilter: received request {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

            // Извлекаем B3 заголовки и создаём span с parent context
            Propagator.Getter<HttpServletRequest> getter = (req, key) -> req.getHeader(key);

            // Проверяем заголовки
            String traceId = httpRequest.getHeader("X-B3-TraceId");
            String spanId = httpRequest.getHeader("X-B3-SpanId");
            log.info("TracingFilter: B3 headers - X-B3-TraceId={}, X-B3-SpanId={}", traceId, spanId);

            // Извлекаем контекст из заголовков и создаём child span
            Span span = propagator.extract(httpRequest, getter).start();
            span.name(httpRequest.getMethod() + " " + httpRequest.getRequestURI());

            log.info("TracingFilter: created span with traceId={}, spanId={}", span.context().traceId(), span.context().spanId());

            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                chain.doFilter(request, response);
            } finally {
                log.info("TracingFilter: ending span");
                span.end();
            }
        }
    }
}
