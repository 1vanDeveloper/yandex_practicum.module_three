package ru.yandex.practicum.cash.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.WithdrawRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class AccountsClient {

    private final RestClient restClient;
    private final Executor executor;
    private final String accountsServiceUrl;

    public AccountsClient(Executor asyncExecutor,
                          @Value("${accounts.service.url:http://accounts:8080}") String accountsServiceUrl) {
        this.restClient = RestClient.builder()
                .requestFactory(createRequestFactory())
                .build();
        this.executor = asyncExecutor;
        this.accountsServiceUrl = accountsServiceUrl;
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return factory;
    }

    public CompletableFuture<Void> deposit(DepositRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/deposit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "amount", request.amount()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> withdraw(WithdrawRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/withdraw";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", request.login(), "amount", request.amount()))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
