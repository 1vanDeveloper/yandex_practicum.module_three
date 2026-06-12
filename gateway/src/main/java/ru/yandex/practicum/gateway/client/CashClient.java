package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.CashRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashClient {

    private final RestTemplate restTemplate;

    private static final String SERVICE_NAME = "cash-service";

    public CompletableFuture<Void> deposit(CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            log.info("CashClient: deposit request for login: {}, amount: {}", request.getLogin(), request.getAmount());
            restTemplate.postForEntity("http://" + SERVICE_NAME + "/cash/deposit", request, Void.class);
        });
    }

    public CompletableFuture<Void> withdraw(CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            log.info("CashClient: withdraw request for login: {}, amount: {}", request.getLogin(), request.getAmount());
            restTemplate.postForEntity("http://" + SERVICE_NAME + "/cash/withdraw", request, Void.class);
        });
    }
}
