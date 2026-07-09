package ru.yandex.practicum.transfer.config;

import brave.sampler.Sampler;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Manual Brave tracing configuration for Zipkin.
 */
@Configuration
public class TracingConfig {

    @Value("${spring.application.name:transfer}")
    private String serviceName;

    @Value("${management.zipkin.tracing.endpoint:http://zipkin:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private float samplingProbability;

    @Bean
    public AsyncZipkinSpanHandler asyncZipkinSpanHandler() {
        var sender = OkHttpSender.create(zipkinEndpoint);
        return AsyncZipkinSpanHandler.create(sender);
    }

    @Bean
    public brave.Tracing braveTracing(AsyncZipkinSpanHandler spanHandler) {
        return brave.Tracing.newBuilder()
            .localServiceName(serviceName)
            .sampler(Sampler.create(samplingProbability))
            .addSpanHandler(spanHandler)
            .build();
    }

    @Bean
    public BraveBaggageManager baggageManager() {
        return new BraveBaggageManager();
    }

    @Bean
    public Tracer tracer(brave.Tracing braveTracing, BraveBaggageManager baggageManager) {
        return new BraveTracer(braveTracing.tracer(), new BraveCurrentTraceContext(braveTracing.currentTraceContext()), baggageManager);
    }

    @Bean
    public Propagator propagator(brave.Tracing braveTracing) {
        return new BravePropagator(braveTracing);
    }

    @Bean
    public CurrentTraceContext currentTraceContext(brave.Tracing braveTracing) {
        return new BraveCurrentTraceContext(braveTracing.currentTraceContext());
    }

    @Bean
    public SpanCustomizer spanCustomizer() {
        return io.micrometer.tracing.SpanCustomizer.NOOP;
    }

    @Bean
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    @Bean
    public PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(Tracer tracer, Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
    }

    @Bean
    public PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(Tracer tracer, Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
    }

    @Bean
    public ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
        registry.observationConfig().observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));
        registry.observationConfig().observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));
        return registry;
    }
}
