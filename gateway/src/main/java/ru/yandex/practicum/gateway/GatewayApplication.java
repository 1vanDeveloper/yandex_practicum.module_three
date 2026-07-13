package ru.yandex.practicum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        // Включаем автоматическую propagation контекста для реактивного tracing
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(GatewayApplication.class, args);
    }
}
