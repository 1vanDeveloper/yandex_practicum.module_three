package ru.yandex.practicum.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "From login is required")
        String fromLogin,

        @NotBlank(message = "To login is required")
        String toLogin,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String comment
) {
}
