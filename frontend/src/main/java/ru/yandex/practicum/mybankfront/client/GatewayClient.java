package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class GatewayClient {

    private final WebClient webClient;

    public CompletableFuture<AccountResponse> getAccount(String gatewayUrl, String login) {
        return webClient.get()
                .uri(gatewayUrl + "/gateway/account")
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .toFuture();
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String gatewayUrl,
            String firstName,
            String lastName,
            String birthDate) {
        return webClient.post()
                .uri(gatewayUrl + "/gateway/account")
                .bodyValue(new UpdateAccountRequest(firstName, lastName, birthDate))
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .toFuture();
    }

    public CompletableFuture<Void> processCash(
            String gatewayUrl,
            Integer value,
            String action) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(gatewayUrl + "/gateway/cash")
                        .queryParam("value", value)
                        .queryParam("action", action)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    public CompletableFuture<Void> processTransfer(
            String gatewayUrl,
            Integer value,
            String login) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(gatewayUrl + "/gateway/transfer")
                        .queryParam("value", value)
                        .queryParam("login", login)
                        .build())
                .retrieve()
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    private record UpdateAccountRequest(String firstName, String lastName, String birthDate) {}
}
