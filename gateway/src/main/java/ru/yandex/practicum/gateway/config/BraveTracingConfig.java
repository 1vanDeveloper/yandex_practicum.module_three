package ru.yandex.practicum.gateway.config;

import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация B3 propagation для совместимости со старыми сервисами.
 * Принудительно используем B3 Multi Header формат (X-B3-TraceId, X-B3-SpanId, X-B3-Sampled)
 * вместо W3C traceparent по умолчанию.
 */
@Configuration
public class BraveTracingConfig {

    @Bean
    public Propagation.Factory b3PropagationFactory() {
        // Принудительно заставляем Micrometer использовать старый X-B3-* multi-header формат
        // для совместимости с downstream сервисами на Spring Cloud Sleuth
        return B3Propagation.newFactoryBuilder()
                .injectFormat(B3Propagation.Format.MULTI)
                .build();
    }
}
