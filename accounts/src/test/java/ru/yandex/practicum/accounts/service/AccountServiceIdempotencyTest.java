package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.accounts.dto.InternalBalanceRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.entity.ProcessedOperation;
import ru.yandex.practicum.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.accounts.repository.AccountRepository;
import ru.yandex.practicum.accounts.repository.ProcessedOperationRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Интеграционные тесты идемпотентности для AccountService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Idempotency Integration Tests")
class AccountServiceIdempotencyTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedOperationRepository processedOperationRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private AccountService accountService;

    private String operationId;
    private InternalBalanceRequest request;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                null, // mapper не нужен для этих тестов
                null, // outboxService не нужен
                null, // passwordEncoder не нужен
                idempotencyService
        );

        operationId = "cash-deposit-user1-" + System.currentTimeMillis();
        request = InternalBalanceRequest.builder()
                .login("user1")
                .amount(new BigDecimal("100.00"))
                .operationId(operationId)
                .sourceService("cash")
                .build();
    }

    @Test
    @DisplayName("Повторный вызов deposit с тем же operationId не должен выполнить операцию")
    void deposit_duplicateOperationId_shouldSkip() {
        // Arrange: операция уже обработана
        when(idempotencyService.isAlreadyProcessed(operationId)).thenReturn(true);

        // Act
        accountService.deposit(request);

        // Assert: репозиторий не должен был вызываться
        verify(accountRepository, never()).findByLogin(anyString());
        verify(idempotencyService).isAlreadyProcessed(operationId);
        verify(idempotencyService, never()).markAsCompleted(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Первый вызов deposit с новым operationId должен выполниться")
    void deposit_newOperationId_shouldExecute() {
        // Arrange: операция ещё не обработана
        when(idempotencyService.isAlreadyProcessed(operationId)).thenReturn(false);
        
        Account account = new Account();
        account.setLogin("user1");
        account.setAmount(new BigDecimal("500.00"));
        
        when(accountRepository.findByLogin("user1")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(idempotencyService.markAsCompleted(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);

        // Act
        accountService.deposit(request);

        // Assert
        verify(accountRepository).findByLogin("user1");
        verify(accountRepository).save(any(Account.class));
        verify(idempotencyService).markAsCompleted(operationId, "DEPOSIT", "user1", "cash");
    }

    @Test
    @DisplayName("При ошибке InsufficientFundsException operationId должен быть сохранён как FAILED")
    void withdraw_insufficientFunds_shouldMarkAsFailed() {
        // Arrange
        String withdrawOpId = "cash-withdraw-user1-" + System.currentTimeMillis();
        InternalBalanceRequest withdrawRequest = InternalBalanceRequest.builder()
                .login("user1")
                .amount(new BigDecimal("1000.00"))
                .operationId(withdrawOpId)
                .sourceService("cash")
                .build();

        when(idempotencyService.isAlreadyProcessed(withdrawOpId)).thenReturn(false);
        
        Account account = new Account();
        account.setLogin("user1");
        account.setAmount(new BigDecimal("50.00")); // Недостаточно средств
        
        when(accountRepository.findByLogin("user1")).thenReturn(Optional.of(account));
        
        ProcessedOperation failedOp = ProcessedOperation.builder()
                .operationId(withdrawOpId)
                .operationType("WITHDRAW")
                .accountLogin("user1")
                .sourceService("cash")
                .status("FAILED")
                .build();
        when(idempotencyService.markAsFailed(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(failedOp);

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () -> accountService.withdraw(withdrawRequest));
        
        verify(idempotencyService).markAsFailed(withdrawOpId, "WITHDRAW", "user1", "cash");
    }
}
