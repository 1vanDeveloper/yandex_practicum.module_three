package ru.yandex.practicum.transfer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
     * Формат: transfer-{type}-{login}-{timestamp}-{uuid}
     */
    private String generateOperationId(String type, String login) {
        return String.format("transfer-%s-%s-%d-%s",
                type.toLowerCase(),
                login,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    public CompletableFuture<Void> debitAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/debit";
            String operationId = generateOperationId("debit", login);

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of(
                        "login", login,
                        "amount", amount,
                        "operationId", operationId,
                        "sourceService", "transfer"))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }

    public CompletableFuture<Void> creditAccount(String login, java.math.BigDecimal amount, String bearerToken) {
        return CompletableFuture.runAsync(() -> {
            String url = accountsServiceUrl + "/accounts/internal/credit";
            String operationId = generateOperationId("credit", login);

            restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of(
                        "login", login,
                        "amount", amount,
                        "operationId", operationId,
                        "sourceService", "transfer"))
                .retrieve()
                .toBodilessEntity();
        }, executor);
    }
}
