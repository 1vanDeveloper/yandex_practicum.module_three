package ru.yandex.practicum.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashRequest(
        @NotBlank(message = "Login is required")
        String login,

        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be positive")
        BigDecimal amount
) {
}
