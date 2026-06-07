package ru.yandex.practicum.transfer.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.transfer.dto.TransferResponse;
import ru.yandex.practicum.transfer.entity.Transfer;
import ru.yandex.practicum.transfer.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransferMapperTest {

    private TransferMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransferMapper();
    }

    @Test
    void testToResponse_whenTransferExists_returnsResponse() {
        // Given
        ZonedDateTime now = ZonedDateTime.now();
        Transfer transfer = new Transfer();
        transfer.setId(1L);
        transfer.setFromAccountLogin("sender_user");
        transfer.setToAccountLogin("receiver_user");
        transfer.setAmount(new BigDecimal("100.50"));
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setErrorMessage(null);
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);

        // When
        TransferResponse response = mapper.toResponse(transfer);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("sender_user", response.fromAccountLogin());
        assertEquals("receiver_user", response.toAccountLogin());
        assertEquals(new BigDecimal("100.50"), response.amount());
        assertEquals(TransferStatus.COMPLETED, response.status());
        assertNull(response.errorMessage());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void testToResponse_whenTransferIsNull_returnsNull() {
        // When
        TransferResponse response = mapper.toResponse(null);

        // Then
        assertNull(response);
    }

    @Test
    void testToResponse_whenFailedTransfer_returnsResponse() {
        // Given
        Transfer transfer = new Transfer();
        transfer.setId(2L);
        transfer.setFromAccountLogin("john_doe");
        transfer.setToAccountLogin("jane_smith");
        transfer.setAmount(new BigDecimal("50.00"));
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setErrorMessage("Insufficient funds");

        // When
        TransferResponse response = mapper.toResponse(transfer);

        // Then
        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals(TransferStatus.FAILED, response.status());
        assertEquals("Insufficient funds", response.errorMessage());
    }
}
