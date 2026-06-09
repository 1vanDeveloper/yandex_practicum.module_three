package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.gateway.dto.CashRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CashClient {

    private final WebClient webClient;

    public CompletableFuture<Void> deposit(String cashUrl, CashRequest request) {
        return webClient.post()
                .uri(cashUrl + "/cash/deposit")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    public CompletableFuture<Void> withdraw(String cashUrl, CashRequest request) {
        return webClient.post()
                .uri(cashUrl + "/cash/withdraw")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .toFuture();
    }
}
