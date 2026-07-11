package ru.yandex.practicum.frontend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ru.yandex.practicum.frontend.dto.AccountBrief;
import ru.yandex.practicum.frontend.dto.AccountResponse;
import ru.yandex.practicum.frontend.dto.JwtTokenResponse;
import ru.yandex.practicum.frontend.dto.LoginRequest;
import ru.yandex.practicum.frontend.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class GatewayClient {

    private final RestClient restClient;
    private final String gatewayServiceUrl;
    private final Executor executor;
    private final ThreadLocal<B3Context> b3Context = new ThreadLocal<>();

    private record B3Context(String traceId, String spanId, String sampled) {}

    public GatewayClient(@Value("${gateway.service.url:http://gateway:8080}") String gatewayServiceUrl) {
        this.gatewayServiceUrl = gatewayServiceUrl;
        this.executor = Executors.newFixedThreadPool(10);
        this.restClient = RestClient.builder()
            .requestInterceptor(b3Interceptor())
            .build();
    }

    /**
     * Добавляет B3 заголовки используя захваченный контекст.
     */
    private void addB3HeadersFromContext(HttpHeaders headers) {
        B3Context ctx = b3Context.get();
        String traceId = ctx.traceId();
        String newSpanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // Если нет trace ID — генерируем новый
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        headers.set("X-B3-TraceId", traceId);
        headers.set("X-B3-SpanId", newSpanId);
        if ("1".equals(ctx.sampled())) {
            headers.set("X-B3-Sampled", "1");
        }
        
        log.debug("GatewayClient: Adding B3 headers - TraceId={}, SpanId={}", traceId, newSpanId);
        b3Context.remove(); // Очищаем после использования
    }

    /**
     * Interceptor для добавления B3 заголовков.
     */
    private org.springframework.http.client.ClientHttpRequestInterceptor b3Interceptor() {
        return (request, body, execution) -> {
            addB3HeadersFromContext(request.getHeaders());
            return execution.execute(request, body);
        };
    }

    private String getGatewayUrl() {
        log.debug("Using gateway URL: {}", gatewayServiceUrl);
        return gatewayServiceUrl;
    }

    @CircuitBreaker(name = "gatewayService", fallbackMethod = "loginFallback")
    public CompletableFuture<JwtTokenResponse> login(LoginRequest request) {
        // Захватываем B3 контекст в текущем потоке
        B3Context capturedContext = captureB3Context();
        String gatewayUrl = getGatewayUrl();
        log.debug("GatewayClient: logging in user: {}", request.getLogin());

        return CompletableFuture.supplyAsync(() -> {
            // Устанавливаем контекст в потоке выполнения
            b3Context.set(capturedContext);
            try {
                return restClient.post()
                    .uri(gatewayUrl + "/gateway/auth/login")
                    .body(request)
                    .retrieve()
                    .body(JwtTokenResponse.class);
            } finally {
                b3Context.remove();
            }
        }, executor);
    }

    /**
     * Захватывает B3 заголовки из текущего запроса.
     */
    private B3Context captureB3Context() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String traceId = request.getHeader("X-B3-TraceId");
            String spanId = request.getHeader("X-B3-SpanId");
            String sampled = request.getHeader("X-B3-Sampled");
            
            // Обрезаем traceId до 32 символов
            if (traceId != null && traceId.length() > 32) {
                traceId = traceId.substring(0, 32);
            }
            
            B3Context ctx = new B3Context(traceId, spanId, sampled);
            log.debug("GatewayClient: Captured B3 context - TraceId={}", traceId);
            return ctx;
        } else {
            log.debug("GatewayClient: No request context available");
            return new B3Context(null, null, null);
        }
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
                .toBodilessEntity()
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
                .body(AccountResponse.class)
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
                .body(AccountResponse.class)
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
                .toBodilessEntity()
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
                .toBodilessEntity()
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
                .body(new ParameterizedTypeReference<List<AccountBrief>>() {})
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
