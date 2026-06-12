package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.TransferRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestTemplate restTemplate;

    @Value("${services.transfer.url:http://transfer-service:8080}")
    private String transferServiceUrl;

    public CompletableFuture<Void> createTransfer(TransferRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(transferServiceUrl + "/transfer", request, Void.class);
        });
    }
}
