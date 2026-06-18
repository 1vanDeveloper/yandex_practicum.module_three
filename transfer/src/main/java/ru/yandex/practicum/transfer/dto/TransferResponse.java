package ru.yandex.practicum.transfer.dto;

import ru.yandex.practicum.transfer.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record TransferResponse(
        Long id,
        String fromAccountLogin,
        String toAccountLogin,
        BigDecimal amount,
        TransferStatus status,
        String errorMessage,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
}
