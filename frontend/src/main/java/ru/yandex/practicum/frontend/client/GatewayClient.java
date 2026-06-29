package ru.yandex.practicum.frontend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ru.yandex.practicum.frontend.dto.AccountBrief;
import ru.yandex.practicum.frontend.dto.AccountResponse;
import ru.yandex.practicum.frontend.dto.JwtTokenResponse;
import ru.yandex.practicum.frontend.dto.LoginRequest;
import ru.yandex.practicum.frontend.dto.RegisterRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class GatewayClient {

    private final RestClient restClient;
    private final Executor executor;
    private final String gatewayServiceUrl;

    public GatewayClient(Executor asyncExecutor,
                         @Value("${gateway.service.url:http://gateway:8080}") String gatewayServiceUrl) {
        this.restClient = RestClient.create();
        this.executor = asyncExecutor;
        this.gatewayServiceUrl = gatewayServiceUrl;
    }

    private String getGatewayUrl() {
        log.debug("Using gateway URL: {}", gatewayServiceUrl);
        return gatewayServiceUrl;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "loginFallback")
    public CompletableFuture<JwtTokenResponse> login(LoginRequest request) {
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: logging in user: {}", request.getLogin());

        return CompletableFuture.supplyAsync(() ->
            restClient.post()
                .uri(gatewayUrl + "/gateway/auth/login")
                .body(request)
                .retrieve()
                .body(JwtTokenResponse.class),
            executor
        );
    }

    public CompletableFuture<JwtTokenResponse> loginFallback(LoginRequest request, Throwable t) {
        log.error("Circuit breaker opened for gateway service (login): {}", t.getMessage());
        CompletableFuture<JwtTokenResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Authentication service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "registerFallback")
    public CompletableFuture<Void> register(RegisterRequest request) {
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: registering user: {}", request.getLogin());

        return CompletableFuture.runAsync(() ->
            restClient.post()
                .uri(gatewayUrl + "/gateway/auth/register")
                .body(request)
                .retrieve()
                .toBodilessEntity(),
            executor
        );
    }

    public CompletableFuture<Void> registerFallback(RegisterRequest request, Throwable t) {
        log.error("Circuit breaker opened for gateway service (register): {}", t.getMessage());
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Registration service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "getAccountFallback")
    public CompletableFuture<AccountResponse> getAccount(String jwtToken) {
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: getting account with provided token");

        if (jwtToken == null) {
            CompletableFuture<AccountResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("JWT token is null"));
            return failedFuture;
        }

        return CompletableFuture.supplyAsync(() ->
            restClient.get()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .body(AccountResponse.class),
            executor
        );
    }

    public CompletableFuture<AccountResponse> getAccountFallback(String jwtToken, Throwable t) {
        log.error("Circuit breaker opened for gateway service (getAccount): {}", t.getMessage());
        CompletableFuture<AccountResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Account service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "updateAccountFallback")
    public CompletableFuture<AccountResponse> updateAccount(
            String firstName,
            String lastName,
            String birthDate,
            String jwtToken) {
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: updating account with provided token");

        if (jwtToken == null) {
            CompletableFuture<AccountResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("JWT token is null"));
            return failedFuture;
        }

        return CompletableFuture.supplyAsync(() ->
            restClient.put()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + jwtToken)
                .body(new UpdateAccountRequest(firstName, lastName, birthDate))
                .retrieve()
                .body(AccountResponse.class),
            executor
        );
    }

    public CompletableFuture<AccountResponse> updateAccountFallback(String firstName, String lastName,
            String birthDate, String jwtToken, Throwable t) {
        log.error("Circuit breaker opened for gateway service (updateAccount): {}", t.getMessage());
        CompletableFuture<AccountResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Account update service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "processCashFallback")
    public CompletableFuture<Void> processCash(Integer value, String action, String jwtToken) {
        String gatewayUrl = getGatewayUrl();
        String url = gatewayUrl + "/gateway/cash?value=" + value + "&action=" + action;
        log.debug("GatewayClient: processing cash action: {} with provided token", action);

        if (jwtToken == null) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("JWT token is null"));
            return failedFuture;
        }

        return CompletableFuture.runAsync(() ->
            restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toBodilessEntity(),
            executor
        );
    }

    public CompletableFuture<Void> processCashFallback(Integer value, String action, String jwtToken, Throwable t) {
        log.error("Circuit breaker opened for gateway service (cash): {}", t.getMessage());
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Cash service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "processTransferFallback")
    public CompletableFuture<Void> processTransfer(Integer value, String toLogin, String jwtToken) {
        String gatewayUrl = getGatewayUrl();
        String url = gatewayUrl + "/gateway/transfer?value=" + value + "&login=" + toLogin;
        log.debug("GatewayClient: processing transfer to: {} with provided token", toLogin);

        if (jwtToken == null) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("JWT token is null"));
            return failedFuture;
        }

        return CompletableFuture.runAsync(() ->
            restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toBodilessEntity(),
            executor
        );
    }

    public CompletableFuture<Void> processTransferFallback(Integer value, String toLogin, String jwtToken, Throwable t) {
        log.error("Circuit breaker opened for gateway service (transfer): {}", t.getMessage());
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Transfer service unavailable, please try again later", t));
        return failedFuture;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "getAccountBriefsFallback")
    public CompletableFuture<List<AccountBrief>> getAccountBriefs(String jwtToken) {
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: getting account briefs with provided token");

        if (jwtToken == null) {
            CompletableFuture<List<AccountBrief>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("JWT token is null"));
            return failedFuture;
        }

        return CompletableFuture.supplyAsync(() ->
            restClient.get()
                .uri(gatewayUrl + "/gateway/accounts")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }),
            executor
        );
    }

    public CompletableFuture<List<AccountBrief>> getAccountBriefsFallback(String jwtToken, Throwable t) {
        log.error("Circuit breaker opened for gateway service (getAccountBriefs): {}", t.getMessage());
        CompletableFuture<List<AccountBrief>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Accounts list service unavailable, please try again later", t));
        return failedFuture;
    }

    private record UpdateAccountRequest(String firstName, String lastName, String birthDate) {}
}
