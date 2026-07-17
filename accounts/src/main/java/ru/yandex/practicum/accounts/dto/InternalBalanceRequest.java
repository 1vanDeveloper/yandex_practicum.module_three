package ru.yandex.practicum.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Запрос на внутреннюю операцию с балансом.
 * Используется для межсервисного взаимодействия (cash, transfer → accounts).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBalanceRequest {

    @NotBlank(message = "Login is required")
    private String login;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Уникальный идентификатор операции для обеспечения идемпотентности.
     * Формат: {source-service}-{operation-type}-{entity-id}-{timestamp}
     * Пример: transfer-debit-123-20240101120000
     * 
     * При повторной отправке того же operationId операция не будет выполнена повторно,
     * а вернётся успешный ответ.
     */
    @NotBlank(message = "Operation ID is required for idempotency")
    private String operationId;

    /**
     * Сервис-источник операции (cash, transfer)
     */
    @NotBlank(message = "Source service is required")
    private String sourceService;
}
