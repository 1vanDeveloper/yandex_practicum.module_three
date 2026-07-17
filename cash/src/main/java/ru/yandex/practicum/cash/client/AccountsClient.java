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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class AccountsClient {

    private final RestClient restClient;
    private final Executor executor;
    private final String accountsServiceUrl;

    public AccountsClient(RestClient.Builder restClientBuilder,
                          Executor asyncExecutor,
                          @Value("${accounts.service.url:http://accounts:8080}") String accountsServiceUrl) {
        this.restClient = restClientBuilder
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

    /**
     * Генерирует уникальный operationId для операции.
     * Формат: cash-{type}-{login}-{timestamp}-{uuid}
     */
    private String generateOperationId(String type, String login) {
        return String.format("cash-%s-%s-%d-%s",
                type.toLowerCase(),
                login,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    public CompletableFuture<Void> deposit(DepositRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/deposit";
            String operationId = generateOperationId("deposit", request.login());

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of(
                        "login", request.login(),
                        "amount", request.amount(),
                        "operationId", operationId,
                        "sourceService", "cash"))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> withdraw(WithdrawRequest request, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/withdraw";
            String operationId = generateOperationId("withdraw", request.login());

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of(
                        "login", request.login(),
                        "amount", request.amount(),
                        "operationId", operationId,
                        "sourceService", "cash"))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
