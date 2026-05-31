package ru.yandex.practicum.accounts.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.entity.Account;

@Repository
public interface AccountRepository extends R2dbcRepository<Account, Long> {

    Mono<Account> findByLogin(String login);

    Mono<Boolean> existsByLogin(String login);
}
