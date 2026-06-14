package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.NotificationsClient;
import ru.yandex.practicum.transfer.dto.NotificationRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.exception.AccountNotFoundException;
import ru.yandex.practicum.transfer.exception.InsufficientFundsException;
import ru.yandex.practicum.transfer.exception.SelfTransferException;
import ru.yandex.practicum.transfer.exception.TransferFailedException;
import ru.yandex.practicum.transfer.mapper.TransferMapper;
import ru.yandex.practicum.transfer.repository.TransferRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferMapper mapper;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private String getAccessToken() {
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("transfer-service")
                        .principal("transfer-service")
                        .build()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new TransferFailedException("Failed to obtain access token");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "createTransferFallback")
    @Transactional
    public CompletableFuture<TransferResponse> createTransfer(TransferRequest request) {
        log.info("Processing transfer from {} to {} for amount {}",
                request.fromLogin(), request.toLogin(), request.amount());

        // Validate self-transfer
        if (request.fromLogin().equals(request.toLogin())) {
            CompletableFuture<TransferResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new SelfTransferException("Cannot transfer to the same account"));
            return failedFuture;
        }

        String token = getAccessToken();

        return accountsClient.debitAccount(request.fromLogin(), request.amount(), token)
                .thenCompose(v -> accountsClient.creditAccount(request.toLogin(), request.amount(), token))
                .thenApply(v -> {
                    Transfer transfer = Transfer.builder()
                            .fromAccountLogin(request.fromLogin())
                            .toAccountLogin(request.toLogin())
                            .amount(request.amount())
                            .status(TransferStatus.COMPLETED)
                            .build();
                    return transferRepository.save(transfer);
                })
                .thenCompose(transfer -> {
                    List<CompletableFuture<Void>> notifications = List.of(
                        sendNotificationSafely(request.fromLogin(),
                                "Money transferred: " + request.amount() + " to " + request.toLogin(), token),
                        sendNotificationSafely(request.toLogin(),
                                "Money received: " + request.amount() + " from " + request.fromLogin(), token)
                    );
                    return CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0]))
                            .thenApply(v -> transfer);
                })
                .thenApply(mapper::toResponse)
                .exceptionally(ex -> {
                    log.error("Transfer failed from {} to {}: {}", request.fromLogin(), request.toLogin(), ex.getMessage());

                    Transfer failedTransfer = Transfer.builder()
                            .fromAccountLogin(request.fromLogin())
                            .toAccountLogin(request.toLogin())
                            .amount(request.amount())
                            .status(TransferStatus.FAILED)
                            .errorMessage(ex.getMessage())
                            .build();
                    transferRepository.save(failedTransfer);

                    if (ex.getCause() instanceof InsufficientFundsException ||
                        ex.getCause() instanceof AccountNotFoundException ||
                        ex.getCause() instanceof SelfTransferException) {
                        throw (RuntimeException) ex.getCause();
                    }
                    throw new TransferFailedException("Transfer failed: " + ex.getMessage(), ex);
                });
    }

    private CompletableFuture<TransferResponse> createTransferFallback(TransferRequest request, Throwable t) {
        log.error("Circuit breaker opened for accounts service (transfer): {}", t.getMessage());
        CompletableFuture<TransferResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new TransferFailedException("Accounts service unavailable, please try again later", t));
        return failedFuture;
    }

    private CompletableFuture<Void> sendNotificationSafely(String login, String message, String token) {
        try {
            return notificationsClient.sendNotification(new NotificationRequest(login, message), token);
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}
