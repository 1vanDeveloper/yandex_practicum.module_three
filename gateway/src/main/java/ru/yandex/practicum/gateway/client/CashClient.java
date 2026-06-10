package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.CashRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class CashClient {

    private final RestTemplate restTemplate;

    public CompletableFuture<Void> deposit(String cashUrl, CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(cashUrl + "/cash/deposit", request, Void.class);
        });
    }

    public CompletableFuture<Void> withdraw(String cashUrl, CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(cashUrl + "/cash/withdraw", request, Void.class);
        });
    }
}
