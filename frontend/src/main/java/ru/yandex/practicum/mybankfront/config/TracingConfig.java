package ru.yandex.practicum.mybankfront.config;

import brave.Tracing;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Конфигурация Micrometer Tracing для frontend сервиса.
 */
@Configuration
public class TracingConfig {

    @Value("${management.zipkin.tracing.endpoint:http://zipkin:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public AsyncZipkinSpanHandler asyncZipkinSpanHandler() {
        OkHttpSender sender = OkHttpSender.newBuilder()
            .endpoint(zipkinEndpoint)
            .build();
        return AsyncZipkinSpanHandler.newBuilder(sender).build();
    }

    @Bean
    public Tracing braveTracing(AsyncZipkinSpanHandler spanHandler) {
        return brave.Tracing.newBuilder()
            .localServiceName("frontend")
            .addSpanHandler(spanHandler)
            .build();
    }

    @Bean
    public Tracer micrometerTracer(Tracing tracing) {
        return new io.micrometer.tracing.brave.bridge.BraveTracer(
            tracing.tracer(),
            new BraveCurrentTraceContext(tracing.currentTraceContext())
        );
    }

    @Bean
    public Propagator propagator(Tracing tracing) {
        return new BravePropagator(tracing);
    }

    @Bean
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    @Bean
    public PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(
        Tracer tracer, Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
    }

    @Bean
    public PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(
        Tracer tracer, Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
    }
}
