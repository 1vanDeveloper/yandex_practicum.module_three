package ru.yandex.practicum.frontend.config;

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
 * Конфигурация Observation для сквозной трассировки.
 * Обеспечивает propagation контекста между HTTP запросами.
 */
@Configuration
public class ObservationConfig {

    @Bean
    public ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
        ObservationRegistry registry = ObservationRegistry.create();
        
        // Обработчик для создания span для observation
        registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
        
        // Обработчик для входящих запросов (receiver) - извлекает B3 заголовки
        registry.observationConfig().observationHandler(new PropagatingReceiverTracingObservationHandler<>(tracer, propagator));
        
        // Обработчик для исходящих запросов (sender) - добавляет B3 заголовки
        registry.observationConfig().observationHandler(new PropagatingSenderTracingObservationHandler<>(tracer, propagator));
        
        return registry;
    }
}
