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

    @CircuitBreaker(name = "accountsService")
    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit for login: {}, amount: {}", request.login(), request.amount());

        CashTransaction pendingTransaction = null;
        try {
            // 1. Сначала создаём запись PENDING
            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.DEPOSIT)
                    .amount(request.amount())
                    .status(TransactionStatus.PENDING)
                    .build();
            pendingTransaction = transactionRepository.save(transaction);
            log.info("Deposit transaction created with PENDING status: {}", pendingTransaction.getId());

            // 2. Выполняем внешний вызов
            String token = getAccessToken();
            accountsClient.deposit(request, token).join();

            // 3. Обновляем статус на COMPLETED
            pendingTransaction.setStatus(TransactionStatus.COMPLETED);
            CashTransaction completedTransaction = transactionRepository.save(pendingTransaction);
            log.info("Deposit transaction completed: {}", completedTransaction.getId());

            sendNotificationSafely(request.login(), "Deposit completed: " + request.amount(), "DEPOSIT");

            return mapper.toResponse(completedTransaction);

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            // Обновляем PENDING → FAILED
            if (pendingTransaction != null) {
                pendingTransaction.setStatus(TransactionStatus.FAILED);
                pendingTransaction.setErrorMessage(e.getMessage());
                transactionRepository.save(pendingTransaction);
            }
            throw e;
        } catch (Exception e) {
            log.error("Deposit failed for login: {}", request.login(), e);
            // Обновляем PENDING → FAILED
            if (pendingTransaction != null) {
                pendingTransaction.setStatus(TransactionStatus.FAILED);
                pendingTransaction.setErrorMessage(e.getMessage());
                transactionRepository.save(pendingTransaction);
            }
            throw new TransactionFailedException("Deposit failed: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "accountsService")
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        log.info("Processing withdrawal for login: {}, amount: {}", request.login(), request.amount());

        CashTransaction pendingTransaction = null;
        try {
            // 1. Сначала создаём запись PENDING
            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(request.amount())
                    .status(TransactionStatus.PENDING)
                    .build();
            pendingTransaction = transactionRepository.save(transaction);
            log.info("Withdrawal transaction created with PENDING status: {}", pendingTransaction.getId());

            // 2. Выполняем внешний вызов
            String token = getAccessToken();
            accountsClient.withdraw(request, token).join();

            // 3. Обновляем статус на COMPLETED
            pendingTransaction.setStatus(TransactionStatus.COMPLETED);
            CashTransaction completedTransaction = transactionRepository.save(pendingTransaction);
            log.info("Withdrawal transaction completed: {}", completedTransaction.getId());

            sendNotificationSafely(request.login(), "Withdrawal completed: " + request.amount(), "WITHDRAW");

            return mapper.toResponse(completedTransaction);

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            // Обновляем PENDING → FAILED
            if (pendingTransaction != null) {
                pendingTransaction.setStatus(TransactionStatus.FAILED);
                pendingTransaction.setErrorMessage(e.getMessage());
                transactionRepository.save(pendingTransaction);
            }
            throw e;
        } catch (Exception e) {
            log.error("Withdrawal failed for login: {}", request.login(), e);
            // Обновляем PENDING → FAILED
            if (pendingTransaction != null) {
                pendingTransaction.setStatus(TransactionStatus.FAILED);
                pendingTransaction.setErrorMessage(e.getMessage());
                transactionRepository.save(pendingTransaction);
            }
            throw new TransactionFailedException("Withdrawal failed: " + e.getMessage(), e);
        }
    }

    private void sendNotificationSafely(String login, String message, String transactionType) {
        try {
            CashNotificationEvent event = CashNotificationEvent.create(
                    login,
                    message,
                    transactionType
            );
            kafkaNotificationSender.sendNotificationSync(event);
            log.debug("Событие нотификации отправлено в Kafka: login={}, message={}", login, message);
        } catch (Exception e) {
            log.warn("Не удалось отправить событие нотификации в Kafka: login={}, message={}, error={}",
                    login, message, e.getMessage());
            // Не пробрасываем исключение, чтобы не прерывать основной поток обработки
        }
    }
}
