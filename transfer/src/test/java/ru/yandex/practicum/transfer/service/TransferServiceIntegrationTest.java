package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.transfer.config.IntegrationTestConfig;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;
import ru.yandex.practicum.transfer.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransferService using PostgreSQL from docker-compose.
 * 
 * Перед запуском убедитесь, что сервисы запущены:
 * docker-compose up -d postgres
 * 
 * Tests verify database interactions with real PostgreSQL instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Import(IntegrationTestConfig.class)
class TransferServiceIntegrationTest {

    @Autowired
    private TransferRepository transferRepository;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
    }

    @Test
    @DisplayName("Сохранение транзакции перевода в БД")
    void transfer_shouldSaveTransferToDatabase() {
        Transfer transfer = Transfer.builder()
            .fromAccountLogin("sender_user")
            .toAccountLogin("receiver_user")
            .amount(new BigDecimal("500.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer saved = transferRepository.save(transfer);

        assertNotNull(saved.getId());
        assertEquals("sender_user", saved.getFromAccountLogin());
        assertEquals("receiver_user", saved.getToAccountLogin());
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
        assertEquals(TransferStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("Поиск переводов по отправителю")
    void findByFromAccountLogin_shouldReturnTransfers() {
        Transfer transfer1 = Transfer.builder()
            .fromAccountLogin("sender1")
            .toAccountLogin("receiver1")
            .amount(new BigDecimal("100.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer transfer2 = Transfer.builder()
            .fromAccountLogin("sender1")
            .toAccountLogin("receiver2")
            .amount(new BigDecimal("200.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        transferRepository.save(transfer1);
        transferRepository.save(transfer2);

        List<Transfer> transfers = transferRepository.findByFromAccountLoginOrderByCreatedAtDesc("sender1");

        assertEquals(2, transfers.size());
        assertTrue(transfers.stream().allMatch(t -> "sender1".equals(t.getFromAccountLogin())));
    }

    @Test
    @DisplayName("Поиск переводов по получателю")
    void findByToAccountLogin_shouldReturnTransfers() {
        Transfer transfer1 = Transfer.builder()
            .fromAccountLogin("sender1")
            .toAccountLogin("receiver1")
            .amount(new BigDecimal("100.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer transfer2 = Transfer.builder()
            .fromAccountLogin("sender2")
            .toAccountLogin("receiver1")
            .amount(new BigDecimal("200.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        transferRepository.save(transfer1);
        transferRepository.save(transfer2);

        List<Transfer> transfers = transferRepository.findByToAccountLoginOrderByCreatedAtDesc("receiver1");

        assertEquals(2, transfers.size());
        assertTrue(transfers.stream().allMatch(t -> "receiver1".equals(t.getToAccountLogin())));
    }

    @Test
    @DisplayName("Поиск переводов по отправителю или получателю")
    void findByFromAccountLoginOrToAccountLogin_shouldReturnTransfers() {
        Transfer transfer1 = Transfer.builder()
            .fromAccountLogin("user1")
            .toAccountLogin("user2")
            .amount(new BigDecimal("100.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer transfer2 = Transfer.builder()
            .fromAccountLogin("user2")
            .toAccountLogin("user3")
            .amount(new BigDecimal("200.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer transfer3 = Transfer.builder()
            .fromAccountLogin("user3")
            .toAccountLogin("user1")
            .amount(new BigDecimal("300.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        transferRepository.save(transfer1);
        transferRepository.save(transfer2);
        transferRepository.save(transfer3);

        List<Transfer> transfers = transferRepository.findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc("user1", "user1");

        assertEquals(2, transfers.size());
        assertTrue(transfers.stream().allMatch(t -> 
            "user1".equals(t.getFromAccountLogin()) || "user1".equals(t.getToAccountLogin())
        ));
    }

    @Test
    @DisplayName("Перевод с ошибкой сохраняется со статусом FAILED")
    void failedTransfer_shouldSaveWithFailedStatus() {
        Transfer transfer = Transfer.builder()
            .fromAccountLogin("sender_user")
            .toAccountLogin("receiver_user")
            .amount(new BigDecimal("500.00"))
            .status(TransferStatus.FAILED)
            .errorMessage("Insufficient funds")
            .build();

        Transfer saved = transferRepository.save(transfer);

        assertEquals(TransferStatus.FAILED, saved.getStatus());
        assertEquals("Insufficient funds", saved.getErrorMessage());
    }

    @Test
    @DisplayName("Перевод со статусом PENDING для повторной обработки")
    void pendingTransfer_shouldSaveWithPendingStatus() {
        Transfer transfer = Transfer.builder()
            .fromAccountLogin("sender_user")
            .toAccountLogin("receiver_user")
            .amount(new BigDecimal("500.00"))
            .status(TransferStatus.PENDING)
            .errorMessage("Accounts service temporarily unavailable")
            .build();

        Transfer saved = transferRepository.save(transfer);

        assertEquals(TransferStatus.PENDING, saved.getStatus());
        assertEquals("Accounts service temporarily unavailable", saved.getErrorMessage());
    }

    @Test
    @DisplayName("Временная метка устанавливается при сохранении")
    void createdAt_shouldBeSetOnSave() {
        ZonedDateTime before = ZonedDateTime.now();

        Transfer transfer = Transfer.builder()
            .fromAccountLogin("sender_user")
            .toAccountLogin("receiver_user")
            .amount(new BigDecimal("100.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer saved = transferRepository.save(transfer);

        ZonedDateTime after = ZonedDateTime.now();

        assertTrue(saved.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(saved.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    @DisplayName("Переводы сортируются по createdAt по убыванию")
    void findByFromAccountLogin_shouldReturnTransfersSortedByCreatedAtDesc() {
        Transfer transfer1 = Transfer.builder()
            .fromAccountLogin("sender1")
            .toAccountLogin("receiver1")
            .amount(new BigDecimal("100.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        Transfer transfer2 = Transfer.builder()
            .fromAccountLogin("sender1")
            .toAccountLogin("receiver2")
            .amount(new BigDecimal("200.00"))
            .status(TransferStatus.COMPLETED)
            .build();

        transferRepository.save(transfer1);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        transferRepository.save(transfer2);

        List<Transfer> transfers = transferRepository.findByFromAccountLoginOrderByCreatedAtDesc("sender1");

        assertEquals(2, transfers.size());
        // Первая транзакция должна быть новее (transfer2)
        assertEquals(transfer2.getId(), transfers.get(0).getId());
    }
}
