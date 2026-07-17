package ru.yandex.practicum.accounts.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Сущность для хранения обработанных operationId.
 * Используется для обеспечения идемпотентности межсервисных операций.
 */
@Entity
@Table(name = "processed_operations", 
       indexes = @Index(name = "idx_operation_id", columnList = "operationId"),
       uniqueConstraints = @UniqueConstraint(name = "uk_operation_id", columnNames = "operationId"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный идентификатор операции.
     * Формат: {source-service}-{operation-type}-{entity-id}-{timestamp}
     * Пример: transfer-debit-123-20240101120000
     */
    @Column(name = "operation_id", nullable = false, length = 255)
    private String operationId;

    /**
     * Тип операции: DEBIT, CREDIT, DEPOSIT, WITHDRAW
     */
    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    /**
     * Логин аккаунта, к которому применяется операция
     */
    @Column(name = "account_login", nullable = false, length = 255)
    private String accountLogin;

    /**
     * Время обработки операции
     */
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    /**
     * Статус обработки: COMPLETED, FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Источник операции: cash, transfer
     */
    @Column(name = "source_service", nullable = false, length = 50)
    private String sourceService;
}
