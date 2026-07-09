package ru.yandex.practicum.gateway.config;

import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация ObservationRegistry для Spring Cloud Gateway.
 */
@Configuration
public class ObservationConfig {

    @Bean
    public ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
        ObservationRegistry registry = ObservationRegistry.create();
        
        // Добавляем handlers для обработки observation
        registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
        registry.observationConfig().observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));
        registry.observationConfig().observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));
        
        return registry;
    }
}
