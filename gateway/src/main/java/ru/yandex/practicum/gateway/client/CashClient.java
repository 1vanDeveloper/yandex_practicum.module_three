package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.CashRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashClient {

    private final RestTemplate restTemplate;

    @Value("${services.cash.url:http://cash-service:8080}")
    private String cashServiceUrl;

    public CompletableFuture<Void> deposit(CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            log.info("CashClient: deposit request for login: {}, amount: {}", request.login(), request.amount());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CashRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(cashServiceUrl + "/cash/deposit", entity, Void.class);
        });
    }

    public CompletableFuture<Void> withdraw(CashRequest request) {
        return CompletableFuture.runAsync(() -> {
            log.info("CashClient: withdraw request for login: {}, amount: {}", request.login(), request.amount());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CashRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(cashServiceUrl + "/cash/withdraw", entity, Void.class);
        });
    }
}
