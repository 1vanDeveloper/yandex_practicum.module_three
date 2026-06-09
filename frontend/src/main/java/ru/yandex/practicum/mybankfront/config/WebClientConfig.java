package ru.yandex.practicum.mybankfront.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${gateway.service.url:http://localhost:8086}")
    private String gatewayUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }
}
