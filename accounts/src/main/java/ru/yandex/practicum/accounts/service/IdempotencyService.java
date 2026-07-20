package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.entity.ProcessedOperation;
import ru.yandex.practicum.accounts.repository.ProcessedOperationRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Сервис для обеспечения идемпотентности межсервисных операций.
 * Проверяет и сохраняет обработанные operationId для предотвращения дублирования.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final ProcessedOperationRepository repository;

    /**
     * Проверяет, была ли операция с данным operationId уже обработана.
     * 
     * @param operationId уникальный идентификатор операции
     * @return true если операция уже обработана, false если нет
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String operationId) {
        return repository.existsByOperationId(operationId);
    }

    /**
     * Проверяет наличие операции и возвращает информацию о ней.
     * 
     * @param operationId уникальный идентификатор операции
     * @return Optional с информацией об операции, если найдена
     */
    @Transactional(readOnly = true)
    public Optional<ProcessedOperation> getProcessedOperation(String operationId) {
        return repository.findByOperationId(operationId);
    }

    /**
     * Сохраняет информацию об обработанной операции.
     * 
     * @param operationId уникальный идентификатор операции
     * @param operationType тип операции (DEBIT, CREDIT, DEPOSIT, WITHDRAW)
     * @param accountLogin логин аккаунта
     * @param sourceService сервис-источник (cash, transfer)
     * @param статус статус обработки (COMPLETED, FAILED)
     * @return сохранённая сущность ProcessedOperation
     */
    @Transactional
    public ProcessedOperation markAsProcessed(
            String operationId,
            String operationType,
            String accountLogin,
            String sourceService,
            String status) {
        
        log.debug("Marking operation as processed: operationId={}, type={}, login={}, source={}, status={}",
                operationId, operationType, accountLogin, sourceService, status);
        
        ProcessedOperation operation = ProcessedOperation.builder()
                .operationId(operationId)
                .operationType(operationType)
                .accountLogin(accountLogin)
                .sourceService(sourceService)
                .status(status)
                .processedAt(Instant.now())
                .build();
        
        return repository.save(operation);
    }

    /**
     * Сохраняет информацию об успешно завершённой операции.
     */
    @Transactional
    public ProcessedOperation markAsCompleted(
            String operationId,
            String operationType,
            String accountLogin,
            String sourceService) {
        return markAsProcessed(operationId, operationType, accountLogin, sourceService, "COMPLETED");
    }

    /**
     * Сохраняет информацию о неудачной операции.
     */
    @Transactional
    public ProcessedOperation markAsFailed(
            String operationId,
            String operationType,
            String accountLogin,
            String sourceService) {
        return markAsProcessed(operationId, operationType, accountLogin, sourceService, "FAILED");
    }
}
