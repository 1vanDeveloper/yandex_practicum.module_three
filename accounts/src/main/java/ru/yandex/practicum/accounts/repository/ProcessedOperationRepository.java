package ru.yandex.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.accounts.entity.ProcessedOperation;

import java.util.Optional;

@Repository
public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, Long> {

    /**
     * Проверка наличия обработанной операции по operationId
     */
    Optional<ProcessedOperation> findByOperationId(String operationId);

    /**
     * Проверка наличия обработанной операции по operationId (для exists query)
     */
    boolean existsByOperationId(String operationId);
}
