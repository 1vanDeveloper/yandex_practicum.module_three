package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.cash.config.IntegrationTestConfig;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CashService using PostgreSQL from Kubernetes.
 *
 * Перед запуском убедитесь, что настроен port-forward:
 *   kubectl port-forward svc/postgresql 5432:5432 &
 *
 * Tests verify database interactions with real PostgreSQL instance from Kubernetes cluster.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Import(IntegrationTestConfig.class)
class CashServiceIntegrationTest {

    @Autowired
    private CashTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("Сохранение транзакции пополнения в БД")
    void deposit_shouldSaveTransactionToDatabase() {
        CashTransaction transaction = CashTransaction.builder()
            .accountLogin("test_user")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("1000.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        CashTransaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        assertEquals("test_user", saved.getAccountLogin());
        assertEquals(TransactionType.DEPOSIT, saved.getTransactionType());
        assertEquals(new BigDecimal("1000.00"), saved.getAmount());
        assertEquals(TransactionStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Сохранение транзакции снятия в БД")
    void withdraw_shouldSaveTransactionToDatabase() {
        CashTransaction transaction = CashTransaction.builder()
            .accountLogin("test_user")
            .transactionType(TransactionType.WITHDRAW)
            .amount(new BigDecimal("500.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        CashTransaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        assertEquals(TransactionType.WITHDRAW, saved.getTransactionType());
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
    }

    @Test
    @DisplayName("Поиск транзакций по логину пользователя")
    void findByAccountLogin_shouldReturnTransactions() {
        CashTransaction transaction1 = CashTransaction.builder()
            .accountLogin("user1")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("1000.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        CashTransaction transaction2 = CashTransaction.builder()
            .accountLogin("user1")
            .transactionType(TransactionType.WITHDRAW)
            .amount(new BigDecimal("200.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        List<CashTransaction> transactions = transactionRepository.findByAccountLoginOrderByCreatedAtDesc("user1");

        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().allMatch(t -> "user1".equals(t.getAccountLogin())));
    }

    @Test
    @DisplayName("Поиск транзакций по статусу PENDING")
    void findPendingTransactions_shouldReturnPendingTransactions() {
        CashTransaction pending1 = CashTransaction.builder()
            .accountLogin("user1")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("1000.00"))
            .status(TransactionStatus.PENDING)
            .build();

        CashTransaction pending2 = CashTransaction.builder()
            .accountLogin("user2")
            .transactionType(TransactionType.WITHDRAW)
            .amount(new BigDecimal("500.00"))
            .status(TransactionStatus.PENDING)
            .build();

        CashTransaction completed = CashTransaction.builder()
            .accountLogin("user3")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("2000.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        transactionRepository.save(pending1);
        transactionRepository.save(pending2);
        transactionRepository.save(completed);

        List<CashTransaction> pendingTransactions = transactionRepository.findPendingTransactions(TransactionStatus.PENDING, 10);

        assertEquals(2, pendingTransactions.size());
        assertTrue(pendingTransactions.stream().allMatch(t -> t.getStatus() == TransactionStatus.PENDING));
    }

    @Test
    @DisplayName("Транзакция с ошибкой сохраняется с статусом FAILED")
    void failedTransaction_shouldSaveWithFailedStatus() {
        CashTransaction transaction = CashTransaction.builder()
            .accountLogin("test_user")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("1000.00"))
            .status(TransactionStatus.FAILED)
            .errorMessage("Insufficient funds")
            .build();

        CashTransaction saved = transactionRepository.save(transaction);

        assertEquals(TransactionStatus.FAILED, saved.getStatus());
        assertEquals("Insufficient funds", saved.getErrorMessage());
    }

    @Test
    @DisplayName("Временная метка устанавливается при сохранении")
    void createdAt_shouldBeSetOnSave() {
        ZonedDateTime before = ZonedDateTime.now();

        CashTransaction transaction = CashTransaction.builder()
            .accountLogin("test_user")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("100.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        CashTransaction saved = transactionRepository.save(transaction);

        ZonedDateTime after = ZonedDateTime.now();

        assertTrue(saved.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(saved.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("Транзакции сортируются по createdAt по убыванию")
    void findByAccountLogin_shouldReturnTransactionsSortedByCreatedAtDesc() {
        CashTransaction transaction1 = CashTransaction.builder()
            .accountLogin("user1")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("100.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        CashTransaction transaction2 = CashTransaction.builder()
            .accountLogin("user1")
            .transactionType(TransactionType.DEPOSIT)
            .amount(new BigDecimal("200.00"))
            .status(TransactionStatus.COMPLETED)
            .build();

        transactionRepository.save(transaction1);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        transactionRepository.save(transaction2);

        List<CashTransaction> transactions = transactionRepository.findByAccountLoginOrderByCreatedAtDesc("user1");

        assertEquals(2, transactions.size());
        // Первая транзакция должна быть новее (transaction2)
        assertEquals(transaction2.getId(), transactions.get(0).getId());
    }
}
