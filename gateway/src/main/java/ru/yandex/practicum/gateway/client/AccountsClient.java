package ru.yandex.practicum.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.gateway.dto.AccountResponse;
import ru.yandex.practicum.gateway.dto.UpdateAccountRequest;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final WebClient webClient;

    public CompletableFuture<AccountResponse> getAccount(String accountsUrl, String login) {
        return webClient.get()
                .uri(accountsUrl + "/accounts/{login}", login)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .toFuture();
    }

    public CompletableFuture<AccountResponse> updateAccount(String accountsUrl, UpdateAccountRequest request) {
        return webClient.patch()
                .uri(accountsUrl + "/accounts")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .toFuture();
    }
}
