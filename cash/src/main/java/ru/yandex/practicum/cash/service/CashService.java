package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
import ru.yandex.practicum.cash.event.CashNotificationEvent;
import ru.yandex.practicum.cash.exception.AccountNotFoundException;
import ru.yandex.practicum.cash.exception.InsufficientFundsException;
import ru.yandex.practicum.cash.exception.TransactionFailedException;
import ru.yandex.practicum.cash.mapper.CashTransactionMapper;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashService {

    private final CashTransactionRepository transactionRepository;
    private final CashTransactionMapper mapper;
    private final AccountsClient accountsClient;
    private final KafkaNotificationSender kafkaNotificationSender;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private String getAccessToken() {
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("cash-service")
                        .principal("cash-service")
                        .build()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new TransactionFailedException("Failed to obtain access token");
        }

        return authorizedClient.getAccessToken().getTokenValue();
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "depositFallback")
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit for login: {}, amount: {}", request.login(), request.amount());

        try {
            String token = getAccessToken();

            accountsClient.deposit(request, token).join();

            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.DEPOSIT)
                    .amount(request.amount())
                    .status(TransactionStatus.COMPLETED)
                    .build();

            CashTransaction savedTransaction = transactionRepository.save(transaction);
            log.info("Deposit transaction completed: {}", savedTransaction.getId());

            sendNotificationSafely(request.login(), "Deposit completed: " + request.amount(), "DEPOSIT");

            return mapper.toResponse(savedTransaction);

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Deposit failed for login: {}", request.login(), e);
            CashTransaction failedTransaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.DEPOSIT)
                    .amount(request.amount())
                    .status(TransactionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();
            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException("Deposit failed: " + e.getMessage(), e);
        }
    }

    private TransactionResponse depositFallback(DepositRequest request, Throwable t) {
        log.error("Circuit breaker opened for accounts service (deposit): {}", t.getMessage());
        
        // Сохраняем транзакцию со статусом PENDING для последующей обработки
        CashTransaction pendingTransaction = CashTransaction.builder()
                .accountLogin(request.login())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .errorMessage("Accounts service temporarily unavailable. Transaction queued for retry.")
                .build();
        transactionRepository.save(pendingTransaction);
        
        // Возвращаем ответ с информацией о статусе
        return mapper.toResponse(pendingTransaction);
    }

    @CircuitBreaker(name = "accountsService", fallbackMethod = "withdrawFallback")
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        log.info("Processing withdrawal for login: {}, amount: {}", request.login(), request.amount());

        try {
            String token = getAccessToken();

            accountsClient.withdraw(request, token).join();

            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(request.amount())
                    .status(TransactionStatus.COMPLETED)
                    .build();

            CashTransaction savedTransaction = transactionRepository.save(transaction);
            log.info("Withdrawal transaction completed: {}", savedTransaction.getId());

            sendNotificationSafely(request.login(), "Withdrawal completed: " + request.amount(), "WITHDRAW");

            return mapper.toResponse(savedTransaction);

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Withdrawal failed for login: {}", request.login(), e);
            CashTransaction failedTransaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(request.amount())
                    .status(TransactionStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();
            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException("Withdrawal failed: " + e.getMessage(), e);
        }
    }

    private TransactionResponse withdrawFallback(WithdrawRequest request, Throwable t) {
        log.error("Circuit breaker opened for accounts service (withdraw): {}", t.getMessage());
        
        // Сохраняем транзакцию со статусом PENDING для последующей обработки
        CashTransaction pendingTransaction = CashTransaction.builder()
                .accountLogin(request.login())
                .transactionType(TransactionType.WITHDRAW)
                .amount(request.amount())
                .status(TransactionStatus.PENDING)
                .errorMessage("Accounts service temporarily unavailable. Transaction queued for retry.")
                .build();
        transactionRepository.save(pendingTransaction);
        
        // Возвращаем ответ с информацией о статусе
        return mapper.toResponse(pendingTransaction);
    }

    private void sendNotificationSafely(String login, String message, String transactionType) {
        try {
            CashNotificationEvent event = CashNotificationEvent.create(
                    login,
                    message,
                    transactionType
            );
            kafkaNotificationSender.sendNotification(event);
            log.debug("Событие нотификации отправлено в Kafka: login={}, message={}", login, message);
        } catch (Exception e) {
            log.warn("Не удалось отправить событие нотификации в Kafka: login={}, message={}, error={}",
                    login, message, e.getMessage());
            // Не пробрасываем исключение, чтобы не прерывать основной поток обработки
        }
    }
}
