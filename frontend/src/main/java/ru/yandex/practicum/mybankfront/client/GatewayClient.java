package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.LoginRequest;
import ru.yandex.practicum.mybankfront.dto.JwtTokenResponse;

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

    private String getJwtToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            return token;
        }
        return null;
    }

    private String getUsernameFromToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String username) {
            return username;
        }
        return null;
    }

    public CompletableFuture<JwtTokenResponse> login(LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            log.debug("GatewayClient: logging in user: {}", request.getLogin());
            return webClient.post()
                .uri(gatewayUrl + "/gateway/auth/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JwtTokenResponse.class)
                .block();
        });
    }

    public CompletableFuture<AccountResponse> getAccount() {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String token = getJwtToken();
            String username = getUsernameFromToken();
            log.debug("GatewayClient: getting account for username: {}", username);
            return webClient.get()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
        });
    }

    public CompletableFuture<AccountResponse> updateAccount(
            String firstName,
            String lastName,
            String birthDate) {
        return CompletableFuture.supplyAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String token = getJwtToken();
            log.debug("GatewayClient: updating account");
            return webClient.put()
                .uri(gatewayUrl + "/gateway/account")
                .header("Authorization", "Bearer " + token)
                .bodyValue(new UpdateAccountRequest(firstName, lastName, birthDate))
                .retrieve()
                .bodyToMono(AccountResponse.class)
                .block();
        });
    }

    public CompletableFuture<Void> processCash(Integer value, String action) {
        return CompletableFuture.runAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String token = getJwtToken();
            String url = gatewayUrl + "/gateway/cash?value=" + value + "&action=" + action;
            log.debug("GatewayClient: processing cash action: {}", action);
            webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        });
    }

    public CompletableFuture<Void> processTransfer(Integer value, String toLogin) {
        return CompletableFuture.runAsync(() -> {
            String gatewayUrl = getGatewayUrl();
            String token = getJwtToken();
            String url = gatewayUrl + "/gateway/transfer?value=" + value + "&login=" + toLogin;
            log.debug("GatewayClient: processing transfer to: {}", toLogin);
            webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        });
    }

    private record UpdateAccountRequest(String firstName, String lastName, String birthDate) {}
}
