package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.NotificationsClient;
import ru.yandex.practicum.cash.dto.DepositRequest;
import ru.yandex.practicum.cash.dto.NotificationRequest;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.dto.WithdrawRequest;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
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
    private final NotificationsClient notificationsClient;
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

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit for login: {}, amount: {}", request.login(), request.amount());
        
        try {
            String token = getAccessToken();
            
            // Call Accounts service to deposit money
            accountsClient.deposit(request, token);
            
            // Create transaction record
            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.DEPOSIT)
                    .amount(request.amount())
                    .status(TransactionStatus.COMPLETED)
                    .build();
            
            CashTransaction savedTransaction = transactionRepository.save(transaction);
            log.info("Deposit transaction completed: {}", savedTransaction.getId());
            
            // Send notification
            try {
                notificationsClient.sendNotification(
                        new NotificationRequest(request.login(), 
                                "Deposit completed: " + request.amount()),
                        token
                );
            } catch (Exception e) {
                log.warn("Failed to send notification for deposit: {}", e.getMessage());
            }
            
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

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        log.info("Processing withdrawal for login: {}, amount: {}", request.login(), request.amount());
        
        try {
            String token = getAccessToken();
            
            // Call Accounts service to withdraw money
            accountsClient.withdraw(request, token);
            
            // Create transaction record
            CashTransaction transaction = CashTransaction.builder()
                    .accountLogin(request.login())
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(request.amount())
                    .status(TransactionStatus.COMPLETED)
                    .build();
            
            CashTransaction savedTransaction = transactionRepository.save(transaction);
            log.info("Withdrawal transaction completed: {}", savedTransaction.getId());
            
            // Send notification
            try {
                notificationsClient.sendNotification(
                        new NotificationRequest(request.login(), 
                                "Withdrawal completed: " + request.amount()),
                        token
                );
            } catch (Exception e) {
                log.warn("Failed to send notification for withdrawal: {}", e.getMessage());
            }
            
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
}
