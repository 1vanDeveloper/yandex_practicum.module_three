package ru.yandex.practicum.transfer.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.practicum.transfer.config.IntegrationTestConfig;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {IntegrationTestConfig.class})
class TransferRepositoryTest {

    @Autowired
    private TransferRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testFindByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc_whenExists_returnsList() {
        // Given
        Transfer tx1 = new Transfer();
        tx1.setFromAccountLogin("sender");
        tx1.setToAccountLogin("receiver");
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setStatus(TransferStatus.COMPLETED);
        repository.save(tx1);

        Transfer tx2 = new Transfer();
        tx2.setFromAccountLogin("receiver");
        tx2.setToAccountLogin("other");
        tx2.setAmount(new BigDecimal("50.00"));
        tx2.setStatus(TransferStatus.COMPLETED);
        repository.save(tx2);

        // When
        List<Transfer> result = repository.findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc("sender", "sender");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFromAccountLogin()).isEqualTo("sender");
    }

    @Test
    void testFindByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc_whenNotExists_returnsEmptyList() {
        // When
        List<Transfer> result = repository.findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc("nonexistent", "nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testSave_whenNewTransfer_savesSuccessfully() {
        // Given
        Transfer newTransfer = new Transfer();
        newTransfer.setFromAccountLogin("new_sender");
        newTransfer.setToAccountLogin("new_receiver");
        newTransfer.setAmount(new BigDecimal("200.00"));
        newTransfer.setStatus(TransferStatus.PENDING);

        // When
        Transfer saved = repository.save(newTransfer);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFromAccountLogin()).isEqualTo("new_sender");
        assertThat(saved.getToAccountLogin()).isEqualTo("new_receiver");
    }
}
