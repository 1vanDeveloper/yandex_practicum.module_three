package ru.yandex.practicum.accounts.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.accounts.entity.OutboxMessage;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxNotificationRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxMessage> findPendingMessages(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxMessage> findPendingMessagesForUpdate(Pageable pageable);

    @Query("SELECT o FROM OutboxMessage o WHERE o.idempotencyKey = :idempotencyKey")
    OutboxMessage findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
