package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.gateway.dto.TransferRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final WebClient webClient;

    public CompletableFuture<Void> createTransfer(String transferUrl, TransferRequest request) {
        return webClient.post()
                .uri(transferUrl + "/transfer")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .toFuture();
    }
}
