package ru.yandex.practicum.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public CompletableFuture<Void> debitAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/debit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", login, "amount", amount))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> creditAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/credit";

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("login", login, "amount", amount))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
