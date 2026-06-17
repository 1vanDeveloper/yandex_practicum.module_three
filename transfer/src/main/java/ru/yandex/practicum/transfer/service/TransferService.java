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
    public TransferResponse createTransfer(TransferRequest request) {
        log.info("Processing transfer from {} to {} for amount {}",
                request.fromLogin(), request.toLogin(), request.amount());

        // Validate self-transfer
        if (request.fromLogin().equals(request.toLogin())) {
            throw new SelfTransferException("Cannot transfer to the same account");
        }

        String token = getAccessToken();

        // Debit from sender
        accountsClient.debitAccount(request.fromLogin(), request.amount(), token).join();

        // Credit to receiver
        accountsClient.creditAccount(request.toLogin(), request.amount(), token).join();

        // Save transfer
        Transfer transfer = Transfer.builder()
                .fromAccountLogin(request.fromLogin())
                .toAccountLogin(request.toLogin())
                .amount(request.amount())
                .status(TransferStatus.COMPLETED)
                .build();
        Transfer savedTransfer = transferRepository.save(transfer);

        // Send notifications (non-blocking, fire-and-forget)
        sendNotificationsSafely(request.fromLogin(), request.toLogin(), request.amount(), token);

        return mapper.toResponse(savedTransfer);
    }

    private void sendNotificationsSafely(String fromLogin, String toLogin, java.math.BigDecimal amount, String token) {
        try {
            List<CompletableFuture<Void>> notifications = List.of(
                notificationsClient.sendNotification(new NotificationRequest(fromLogin,
                        "Money transferred: " + amount + " to " + toLogin), token),
                notificationsClient.sendNotification(new NotificationRequest(toLogin,
                        "Money received: " + amount + " from " + fromLogin), token)
            );
            CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("Failed to send notifications: {}", e.getMessage());
        }
    }

    private TransferResponse createTransferFallback(TransferRequest request, Throwable t) {
        log.error("Circuit breaker triggered for transfer from {} to {}: {}",
                request.fromLogin(), request.toLogin(), t.getMessage());

        Transfer failedTransfer = Transfer.builder()
                .fromAccountLogin(request.fromLogin())
                .toAccountLogin(request.toLogin())
                .amount(request.amount())
                .status(TransferStatus.FAILED)
                .errorMessage("Circuit breaker: " + t.getMessage())
                .build();
        transferRepository.save(failedTransfer);

        throw new TransferFailedException("Transfer failed due to circuit breaker: " + t.getMessage(), t);
    }
}
