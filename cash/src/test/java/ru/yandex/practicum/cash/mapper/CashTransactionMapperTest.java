package ru.yandex.practicum.cash.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.cash.dto.TransactionResponse;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CashTransactionMapperTest {

    private CashTransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CashTransactionMapper();
    }

    @Test
    void testToResponse_whenTransactionExists_returnsResponse() {
        // Given
        ZonedDateTime now = ZonedDateTime.now();
        CashTransaction transaction = new CashTransaction();
        transaction.setId(1L);
        transaction.setAccountLogin("test_user");
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.50"));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setErrorMessage(null);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        // When
        TransactionResponse response = mapper.toResponse(transaction);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test_user", response.accountLogin());
        assertEquals(TransactionType.DEPOSIT, response.transactionType());
        assertEquals(new BigDecimal("100.50"), response.amount());
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertNull(response.errorMessage());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void testToResponse_whenTransactionIsNull_returnsNull() {
        // When
        TransactionResponse response = mapper.toResponse(null);

        // Then
        assertNull(response);
    }

    @Test
    void testToResponse_whenWithdrawTransaction_returnsResponse() {
        // Given
        CashTransaction transaction = new CashTransaction();
        transaction.setId(2L);
        transaction.setAccountLogin("john_doe");
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setErrorMessage("Insufficient funds");

        // When
        TransactionResponse response = mapper.toResponse(transaction);

        // Then
        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("john_doe", response.accountLogin());
        assertEquals(TransactionType.WITHDRAW, response.transactionType());
        assertEquals(new BigDecimal("50.00"), response.amount());
        assertEquals(TransactionStatus.FAILED, response.status());
        assertEquals("Insufficient funds", response.errorMessage());
    }
}
