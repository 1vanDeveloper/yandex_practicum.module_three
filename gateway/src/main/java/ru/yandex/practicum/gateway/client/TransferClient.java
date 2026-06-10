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

    private static final String SERVICE_NAME = "transfer-service";

    public CompletableFuture<Void> createTransfer(TransferRequest request) {
        return CompletableFuture.runAsync(() -> {
            restTemplate.postForEntity("http://" + SERVICE_NAME + "/transfer", request, Void.class);
        });
    }
}
