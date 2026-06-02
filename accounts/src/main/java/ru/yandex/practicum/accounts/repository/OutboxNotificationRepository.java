package ru.yandex.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.accounts.entity.OutboxMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutboxNotificationRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query("SELECT m FROM OutboxMessage m WHERE m.status = 'PENDING' ORDER BY m.createdAt ASC")
    List<OutboxMessage> findPendingMessages(@Param("limit") int limit);

    @Override
    Optional<OutboxMessage> findById(UUID id);
}
