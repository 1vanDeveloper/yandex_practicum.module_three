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

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService Tests")
class IdempotencyServiceTest {

    @Mock
    private ProcessedOperationRepository repository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(repository);
    }

    @Test
    @DisplayName("Должен вернуть true, если операция уже обработана")
    void isAlreadyProcessed_whenExists_shouldReturnTrue() {
        String operationId = "cash-deposit-user1-123456";
        when(repository.existsByOperationId(operationId)).thenReturn(true);

        boolean result = idempotencyService.isAlreadyProcessed(operationId);

        assertTrue(result);
        verify(repository).existsByOperationId(operationId);
    }

    @Test
    @DisplayName("Должен вернуть false, если операция не обработана")
    void isAlreadyProcessed_whenNotExists_shouldReturnFalse() {
        String operationId = "cash-deposit-user1-123456";
        when(repository.existsByOperationId(operationId)).thenReturn(false);

        boolean result = idempotencyService.isAlreadyProcessed(operationId);

        assertFalse(result);
        verify(repository).existsByOperationId(operationId);
    }

    @Test
    @DisplayName("Должен сохранить операцию как завершённую")
    void markAsCompleted_shouldSaveWithCompletedStatus() {
        String operationId = "cash-deposit-user1-123456";
        ProcessedOperation savedOp = ProcessedOperation.builder()
                .operationId(operationId)
                .operationType("DEPOSIT")
                .accountLogin("user1")
                .sourceService("cash")
                .status("COMPLETED")
                .build();

        when(repository.save(any(ProcessedOperation.class))).thenReturn(savedOp);

        ProcessedOperation result = idempotencyService.markAsCompleted(
                operationId, "DEPOSIT", "user1", "cash");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(repository).save(any(ProcessedOperation.class));
    }

    @Test
    @DisplayName("Должен сохранить операцию как неудачную")
    void markAsFailed_shouldSaveWithFailedStatus() {
        String operationId = "transfer-debit-user1-123456";
        ProcessedOperation savedOp = ProcessedOperation.builder()
                .operationId(operationId)
                .operationType("DEBIT")
                .accountLogin("user1")
                .sourceService("transfer")
                .status("FAILED")
                .build();

        when(repository.save(any(ProcessedOperation.class))).thenReturn(savedOp);

        ProcessedOperation result = idempotencyService.markAsFailed(
                operationId, "DEBIT", "user1", "transfer");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        verify(repository).save(any(ProcessedOperation.class));
    }
}
