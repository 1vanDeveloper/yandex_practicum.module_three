package ru.yandex.practicum.cash.dto;

import ru.yandex.practicum.cash.entity.TransactionStatus;
import ru.yandex.practicum.cash.entity.TransactionType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record TransactionResponse(
        Long id,
        String accountLogin,
        TransactionType transactionType,
        BigDecimal amount,
        TransactionStatus status,
        String errorMessage,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
}
