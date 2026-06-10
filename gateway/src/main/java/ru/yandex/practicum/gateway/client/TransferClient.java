package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.yandex.practicum.gateway.dto.TransferRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestTemplate restTemplate;

    public CompletableFuture<Void> createTransfer(String transferUrl, TransferRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity(transferUrl + "/transfer", request, Void.class);
        });
    }
}
