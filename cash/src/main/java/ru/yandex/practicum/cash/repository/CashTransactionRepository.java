package ru.yandex.practicum.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.cash.entity.CashTransaction;
import ru.yandex.practicum.cash.entity.TransactionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {

    List<CashTransaction> findByAccountLoginOrderByCreatedAtDesc(String accountLogin);

    Optional<CashTransaction> findByIdAndAccountLogin(Long id, String accountLogin);

    @Query("SELECT ct FROM CashTransaction ct WHERE ct.status = :status ORDER BY ct.createdAt ASC LIMIT :limit")
    List<CashTransaction> findPendingTransactions(@Param("status") TransactionStatus status, @Param("limit") int limit);
}
