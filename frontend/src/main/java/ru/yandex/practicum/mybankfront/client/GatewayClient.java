package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mybankfront.dto.AccountBrief;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.JwtTokenResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.dto.RegisterRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayClient {

    private final WebClient webClient;
    private final DiscoveryClient discoveryClient;

    private String getAccountsUrl() {
        // Frontend вызывает Gateway, а не accounts напрямую
        List<ServiceInstance> instances = discoveryClient.getInstances("gateway");
        if (instances.isEmpty()) {
            throw new IllegalStateException("No gateway instances found in Consul");
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString() + "/gateway";
    }

    private String getGatewayUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances("gateway");
        if (instances.isEmpty()) {
            throw new IllegalStateException("No gateway instances found in Consul");
        }
        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }

    public CompletableFuture<JwtTokenResponse> login(LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: logging in user: {}", request.getLogin());
            
            // Отправляем как JSON
            return webClient.post()
                .uri(gatewayUrl + "/gateway/auth/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JwtTokenResponse.class)
                .block();
        });
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        return CompletableFuture.runAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: registering user: {}", request.getLogin());
            
            // Отправляем как JSON
            webClient.post()
                .uri(gatewayUrl + "/gateway/auth/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        });
    }

    public CompletableFuture<AccountResponse> getAccount(String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: getting account with provided token");
            
            if (jwtToken == null) {
                throw new IllegalStateException("JWT token is null");
            }
            
            return webClient.get()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String firstName,
            String lastName,
            String birthDate,
            String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: updating account with provided token");
            
            if (jwtToken == null) {
                throw new IllegalStateException("JWT token is null");
            }
            
            return webClient.put()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + jwtToken)
                .bodyValue(new UpdateAccountRequest(firstName, lastName, birthDate))
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
        });
    }

    public CompletableFuture<Void> processCash(Integer value, String action, String jwtToken) {
        return CompletableFuture.runAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String url = gatewayUrl + "/gateway/cash?value=" + value + "&action=" + action;
            log.debug("GatewayClient: processing cash action: {} with provided token", action);
            
            if (jwtToken == null) {
                throw new IllegalStateException("JWT token is null");
            }
            
            webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        });
    }

    public CompletableFuture<Void> processTransfer(Integer value, String toLogin, String jwtToken) {
        return CompletableFuture.runAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String url = gatewayUrl + "/gateway/transfer?value=" + value + "&login=" + toLogin;
            log.debug("GatewayClient: processing transfer to: {} with provided token", toLogin);

            if (jwtToken == null) {
                throw new IllegalStateException("JWT token is null");
            }

            webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        });
    }

    public CompletableFuture<List<AccountBrief>> getAccountBriefs(String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: getting account briefs with provided token");

            if (jwtToken == null) {
                throw new IllegalStateException("JWT token is null");
            }

            return webClient.get()
                .uri(gatewayUrl + "/gateway/accounts")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToFlux(AccountBrief.class)
                .collectList()
                .block();
        });
    }

    private record UpdateAccountRequest(String firstName, String lastName, String birthDate) {}
}
