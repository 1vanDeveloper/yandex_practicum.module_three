package ru.yandex.practicum.cash.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.cash.config.TestSecurityConfig;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CashTransactionRepositoryTest {

    @Autowired
    private CashTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testFindByAccountLoginOrderByCreatedAtDesc_whenExists_returnsList() {
        // Given
        CashTransaction tx1 = new CashTransaction();
        tx1.setAccountLogin("test_user");
        tx1.setTransactionType(TransactionType.DEPOSIT);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setStatus(TransactionStatus.PENDING);
        repository.save(tx1);

        CashTransaction tx2 = new CashTransaction();
        tx2.setAccountLogin("test_user");
        tx2.setTransactionType(TransactionType.WITHDRAW);
        tx2.setAmount(new BigDecimal("50.00"));
        tx2.setStatus(TransactionStatus.COMPLETED);
        repository.save(tx2);

        // When
        List<CashTransaction> result = repository.findByAccountLoginOrderByCreatedAtDesc("test_user");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountLogin()).isEqualTo("test_user");
    }

    @Test
    void testFindByAccountLoginOrderByCreatedAtDesc_whenNotExists_returnsEmptyList() {
        // When
        List<CashTransaction> result = repository.findByAccountLoginOrderByCreatedAtDesc("nonexistent_user");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByIdAndAccountLogin_whenExists_returnsOptional() {
        // Given
        CashTransaction tx = new CashTransaction();
        tx.setAccountLogin("test_user");
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setStatus(TransactionStatus.PENDING);
        tx = repository.save(tx);

        // When
        Optional<CashTransaction> result = repository.findByIdAndAccountLogin(tx.getId(), "test_user");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void testFindByIdAndAccountLogin_whenNotExists_returnsEmpty() {
        // When
        Optional<CashTransaction> result = repository.findByIdAndAccountLogin(999L, "test_user");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testFindByIdAndAccountLogin_whenDifferentLogin_returnsEmpty() {
        // Given
        CashTransaction tx = new CashTransaction();
        tx.setAccountLogin("test_user");
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setStatus(TransactionStatus.PENDING);
        tx = repository.save(tx);

        // When
        Optional<CashTransaction> result = repository.findByIdAndAccountLogin(tx.getId(), "wrong_user");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testSave_whenNewTransaction_savesSuccessfully() {
        // Given
        CashTransaction newTransaction = new CashTransaction();
        newTransaction.setAccountLogin("new_user");
        newTransaction.setTransactionType(TransactionType.WITHDRAW);
        newTransaction.setAmount(new BigDecimal("200.00"));
        newTransaction.setStatus(TransactionStatus.PENDING);

        // When
        CashTransaction saved = repository.save(newTransaction);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountLogin()).isEqualTo("new_user");
    }
}
