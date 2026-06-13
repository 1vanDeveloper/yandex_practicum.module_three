package ru.yandex.practicum.mybankfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .filter(jwtExchangeFilterFunction())
            .build();
    }

    private ExchangeFilterFunction jwtExchangeFilterFunction() {
        return (request, next) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                ClientRequest filteredRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + jwt.getTokenValue())
                    .build();
                return next.exchange(filteredRequest);
            }
            return next.exchange(request);
        };
    }
}
