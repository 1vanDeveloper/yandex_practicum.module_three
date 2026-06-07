package ru.yandex.practicum.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.transfer.entity.Transfer;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByFromAccountLoginOrderByCreatedAtDesc(String fromAccountLogin);

    List<Transfer> findByToAccountLoginOrderByCreatedAtDesc(String toAccountLogin);

    List<Transfer> findByFromAccountLoginOrToAccountLoginOrderByCreatedAtDesc(String fromAccountLogin, String toAccountLogin);
}
