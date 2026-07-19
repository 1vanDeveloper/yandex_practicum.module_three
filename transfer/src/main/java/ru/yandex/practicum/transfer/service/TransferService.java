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
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.event.TransferNotificationEvent;
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
    private final KafkaNotificationSender kafkaNotificationSender;
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

    @CircuitBreaker(name = "accountsService")
    @Transactional
    public TransferResponse createTransfer(TransferRequest request) {
        log.info("Processing transfer from {} to {} for amount {}",
                request.fromLogin(), request.toLogin(), request.amount());

        // Validate self-transfer
        if (request.fromLogin().equals(request.toLogin())) {
            throw new SelfTransferException("Cannot transfer to the same account");
        }

        // 1. Создаём PENDING запись ДО внешних вызовов
        Transfer pendingTransfer = Transfer.builder()
                .fromAccountLogin(request.fromLogin())
                .toAccountLogin(request.toLogin())
                .amount(request.amount())
                .status(TransferStatus.PENDING)
                .build();
        pendingTransfer = transferRepository.save(pendingTransfer);
        log.info("Transfer created with PENDING status: {}", pendingTransfer.getId());

        String token = getAccessToken();

        try {
            // 2. Debit from sender
            accountsClient.debitAccount(request.fromLogin(), request.amount(), request.operationId(), token).join();

            // 3. Credit to receiver
            accountsClient.creditAccount(request.toLogin(), request.amount(), request.operationId(), token).join();

            // 4. Обновляем статус на COMPLETED
            pendingTransfer.setStatus(TransferStatus.COMPLETED);
            Transfer completedTransfer = transferRepository.save(pendingTransfer);
            log.info("Transfer completed: {}", completedTransfer.getId());

            // Send notifications (non-blocking, fire-and-forget via Kafka)
            sendNotificationsSafely(request.fromLogin(), request.toLogin(), request.amount());

            return mapper.toResponse(completedTransfer);

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            // Обновляем PENDING → FAILED
            pendingTransfer.setStatus(TransferStatus.FAILED);
            pendingTransfer.setErrorMessage(e.getMessage());
            transferRepository.save(pendingTransfer);
            throw e;
        } catch (Exception e) {
            log.error("Transfer failed from {} to {}: {}", request.fromLogin(), request.toLogin(), e.getMessage(), e);
            // Обновляем PENDING → FAILED
            pendingTransfer.setStatus(TransferStatus.FAILED);
            pendingTransfer.setErrorMessage(e.getMessage());
            transferRepository.save(pendingTransfer);
            throw new TransferFailedException("Transfer failed: " + e.getMessage(), e);
        }
    }

    private void sendNotificationsSafely(String fromLogin, String toLogin, java.math.BigDecimal amount) {
        try {
            // Отправляем уведомление отправителю
            TransferNotificationEvent fromEvent = TransferNotificationEvent.create(
                    fromLogin,
                    "Money transferred: " + amount + " to " + toLogin,
                    "TRANSFER_SENT"
            );
            kafkaNotificationSender.sendNotificationSync(fromEvent);
            log.debug("Событие нотификации отправлено в Kafka: login={}, message={}", fromLogin, fromEvent.message());

            // Отправляем уведомление получателю
            TransferNotificationEvent toEvent = TransferNotificationEvent.create(
                    toLogin,
                    "Money received: " + amount + " from " + fromLogin,
                    "TRANSFER_RECEIVED"
            );
            kafkaNotificationSender.sendNotificationSync(toEvent);
            log.debug("Событие нотификации отправлено в Kafka: login={}, message={}", toLogin, toEvent.message());
        } catch (Exception e) {
            log.warn("Не удалось отправить события нотификации в Kafka: fromLogin={}, toLogin={}, error={}",
                    fromLogin, toLogin, e.getMessage());
            // Не пробрасываем исключение, чтобы не прерывать основной поток обработки
        }
    }
}
